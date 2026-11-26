pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
        skipStagesAfterUnstable()
    }

    environment {
        JENKINS_CONTAINER = 'cicd-jenkins'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-21'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    set -eu
                    chmod +x ./mvnw
                    ./mvnw -B -ntp clean verify
                '''
            }
        }

        stage('Trivy Security Scan') {
            agent {
                docker {
                    image 'aquasec/trivy:0.74.0'
                    reuseNode true
                    args '--entrypoint="" -e HOME=/tmp -e XDG_CACHE_HOME=/tmp/.cache'
                }
            }
            steps {
                sh '''
                    set -eu
                    mkdir -p .trivy-cache
                    trivy fs \
                        --cache-dir .trivy-cache \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --format sarif \
                        --output trivy-results.sarif \
                        --exit-code 1 \
                        .
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-results.sarif', allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube Analysis') {
            agent {
                docker {
                    image 'sonarsource/sonar-scanner-cli:latest'
                    reuseNode true
                    args '--entrypoint="" --network cicd-network'
                }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'sonar-scanner'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package Application') {
            agent {
                docker {
                    image 'node:24-alpine'
                    reuseNode true
                    args '-u root'
                }
            }
            steps {
                sh '''
                    set -eu
                    apk add --no-cache zip
                    rm -f latest-app.zip
                    zip -r latest-app.zip \
                        Dockerfile docker-compose.yml .dockerignore .env.example \
                        Jenkinsfile mvnw mvnw.cmd pom.xml README.md sonar-project.properties .mvn src \
                        -x 'src/main/frontend/generated/*'
                    ls -lh latest-app.zip
                '''
            }
        }

        stage('Publish Application') {
            steps {
                script {
                    def appName = env.JOB_NAME
                            .replaceAll('[^a-zA-Z0-9._-]', '-')
                            .replaceAll('-+', '-')
                            .replaceAll('^-|-$', '')
                    def buildId = env.BUILD_NUMBER
                    def contentPath = "/var/jenkins_home/userContent/applications/${appName}/${buildId}"

                    sh """
                        set -eu
                        docker exec "${env.JENKINS_CONTAINER}" mkdir -p "${contentPath}"
                        docker cp latest-app.zip "${env.JENKINS_CONTAINER}:${contentPath}/latest-app.zip"
                    """
                    archiveArtifacts artifacts: 'latest-app.zip', fingerprint: true

                    def buildUrl = env.BUILD_URL
                    def jobPath = buildUrl == null ? -1 : buildUrl.indexOf('/job/')
                    if (jobPath < 0) {
                        error('Jenkins BUILD_URL harus berisi /job/ agar artifact dapat dipublikasikan.')
                    }
                    def jenkinsBaseUrl = buildUrl.substring(0, jobPath)
                            .replace('localhost', 'host.docker.internal')
                    env.ARTIFACT_URL = "${jenkinsBaseUrl}/userContent/applications/${appName}/${buildId}/latest-app.zip"
                    echo "Artifact published at ${env.ARTIFACT_URL}"
                }
            }
        }

        stage('Deploy Application') {
            when {
                expression {
                    (env.BRANCH_NAME ?: env.GIT_BRANCH ?: '')
                            .replaceFirst('^origin/', '') == 'main'
                }
            }
            agent {
                docker {
                    image 'curlimages/curl:8.15.0'
                    reuseNode true
                    args '--entrypoint="" --network cicd-network'
                }
            }
            steps {
                script {
                    ['URL_REDEPLOY', 'URL_PROGRESS', 'WEBSITE_ID', 'ARTIFACT_URL'].each { name ->
                        if (!env[name]?.trim()) {
                            error("Konfigurasi Jenkins ${name} belum diisi.")
                        }
                    }

                    withCredentials([string(credentialsId: 'delcom-deploy-token', variable: 'DEPLOY_TOKEN')]) {
                        def redeployRequest = groovy.json.JsonOutput.toJson([
                                token_access: env.DEPLOY_TOKEN,
                                website_id: env.WEBSITE_ID,
                                source_url: env.ARTIFACT_URL,
                                source_type: 'jenkins'
                        ])
                        writeFile file: '.delcom-redeploy.json', text: redeployRequest

                        try {
                            sh '''
                                set -eu
                                chmod 600 .delcom-redeploy.json
                                curl --fail --silent --show-error \
                                    --header 'Content-Type: application/json' \
                                    --data-binary @.delcom-redeploy.json \
                                    "$URL_REDEPLOY" >/dev/null
                            '''

                            timeout(time: 15, unit: 'MINUTES') {
                                def deploymentStatus = 'IN_PROGRESS'
                                for (int attempt = 1; attempt <= 120; attempt++) {
                                    sleep(time: 5, unit: 'SECONDS')
                                    def progressRequest = groovy.json.JsonOutput.toJson([
                                            token_access: env.DEPLOY_TOKEN,
                                            website_id: env.WEBSITE_ID
                                    ])
                                    writeFile file: '.delcom-progress.json', text: progressRequest

                                    try {
                                        def response = sh(
                                                script: '''
                                                    set -eu
                                                    chmod 600 .delcom-progress.json
                                                    curl --fail --silent --show-error \
                                                        --header 'Content-Type: application/json' \
                                                        --data-binary @.delcom-progress.json \
                                                        "$URL_PROGRESS"
                                                ''',
                                                returnStdout: true
                                        ).trim()
                                        def result = readJSON text: response
                                        deploymentStatus = result?.data?.status?.toString()?.toUpperCase()

                                        if (deploymentStatus == 'SUCCESS') {
                                            echo 'Deployment Delcom berhasil.'
                                            break
                                        }
                                        if (deploymentStatus == 'FAIL') {
                                            error("Deployment Delcom gagal: ${result?.data?.log ?: 'tidak ada log dari server'}")
                                        }
                                        if (deploymentStatus != 'IN_PROGRESS') {
                                            error("Status deployment Delcom tidak dikenal: ${deploymentStatus}")
                                        }
                                        echo "Deployment masih berjalan (${attempt}/120)."
                                    } finally {
                                        sh 'rm -f .delcom-progress.json'
                                    }
                                }

                                if (deploymentStatus != 'SUCCESS') {
                                    error('Deployment Delcom melewati batas waktu 15 menit.')
                                }
                            }
                        } finally {
                            sh 'rm -f .delcom-redeploy.json .delcom-progress.json'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/site/jacoco/**,trivy-results.sarif',
                    allowEmptyArchive: true
        }
        success {
            echo "Pipeline berhasil: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
        failure {
            echo 'Pipeline gagal. Periksa stage yang gagal dan Console Output.'
        }
    }
}
