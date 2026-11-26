package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;

/** Preview gambar: klik gambar untuk toggle zoom 1.0x / 1.8x; tombol Perkecil/Perbesar/Tutup di kanan footer. */
public class ImagePreviewDialog extends AppDialog {
    private static final double MIN = 1.0;
    private static final double MAX = 3.0;
    private final Image image;
    private double zoom = 1.0;

    public ImagePreviewDialog(String url, String title) {
        super(title, "Pratinjau");
        setWidth("760px");
        image = new Image(url, title);
        image.addClassName("preview-image");
        image.addClickListener(e -> setZoom(zoom > 1.0 ? 1.0 : 1.8));

        Div box = new Div(image);
        box.addClassName("preview-box");
        add(box);

        Button smaller = new Button("Perkecil", e -> setZoom(Math.max(MIN, zoom - 0.4)));
        Button bigger = new Button("Perbesar", e -> setZoom(Math.min(MAX, zoom + 0.4)));
        Button close = new Button("Tutup", e -> close());
        addActions(smaller, bigger, close);
        setZoom(1.0);
    }

    private void setZoom(double value) {
        zoom = value;
        image.setWidth(Math.round(value * 100) + "%");
    }
}
