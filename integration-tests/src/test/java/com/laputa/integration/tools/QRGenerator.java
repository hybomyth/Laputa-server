package com.laputa.integration.tools;

import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.db.DBManager;
import com.laputa.server.db.model.Redeem;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 22.03.16.
 */
public class QRGenerator {

    public static void main(String[] args) throws Exception {
        DBManager dbManager = new DBManager("db.properties", new BlockingIOProcessor(4, 100), true);
        List<Redeem> redeems;

        redeems = generateQR(361, "E:\\working_idea\\Laputa-server\\laputa_data", "Laputa", 25000);
        dbManager.insertRedeems(redeems);

        redeems = generateQR(1950, "/home/doom369/QR/laputa100", "Laputa", 100000);
        dbManager.insertRedeems(redeems);

        redeems = generateQR(90, "/home/doom369/QR/bluz", "Bluz", 100000);
        dbManager.insertRedeems(redeems);

        redeems = generateQR(210, "/home/doom369/QR/oak", "Digistump Oak", 100000);
        dbManager.insertRedeems(redeems);

        redeems = generateQR(160, "/home/doom369/QR/onion", "Onion Omega", 100000);
        dbManager.insertRedeems(redeems);
    }

    private static List<Redeem> generateQR(int count, String outputFolder, String campaign, int reward) throws Exception {
        List<Redeem> redeems = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String token = UUID.randomUUID().toString().replace("-", "");

            Redeem redeem = new Redeem(token, campaign, reward);
            redeems.add(redeem);

            Path path = Paths.get(outputFolder, String.format("%d.jpg", i));
            generateQR(redeem.formatToken(), path);
        }
        return redeems;
    }

    private static void generateQR(String text, Path outputFile) throws Exception {
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            QRCode.from(text).to(ImageType.JPG).writeTo(out);
        }
    }

}
