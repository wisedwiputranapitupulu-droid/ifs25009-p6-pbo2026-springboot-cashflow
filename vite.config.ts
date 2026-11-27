import type { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

const customConfig: UserConfigFn = () => ({
  build: {
    sourcemap: true,
  },
});

export default overrideVaadinConfig(customConfig);
