package at.omasits.proteomics.protter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.util.PDFMergerUtility;
import at.omasits.proteomics.protter.ProtProvider.ProtFile;
import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.Util;
import at.omasits.util.httpServer.NanoHTTPD;
import at.omasits.util.httpServer.NanoHTTPD.Response;
import at.omasits.util.httpServer.NanoHTTPD.Response.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class BatchProcess extends Thread {
    Map<String,String> parms;
    String batchFormat;
    String batchEmail;
    List<String> batchProteins;

    public BatchProcess(String batchFormat, String batchEmail, List<String> batchProteins, Map<String,String> parms) {
        this.batchFormat = batchFormat;
        this.batchEmail = batchEmail;
        this.batchProteins = batchProteins;
        this.parms = parms;
        this.parms.remove("up");
        this.parms.remove("seq");
        this.parms.remove("title");
        this.parms.remove("peptides");
        this.parms.remove("format");

        String protFormat = "svg";
        if (batchFormat.equals("batchPdf") || batchFormat.equals("batchZippdf"))
            protFormat = "pdf";
        else if (batchFormat.equals("batchZipsvg"))
            protFormat = "svg";
        else if (batchFormat.equals("batchZippng"))
            protFormat = "png";
        this.parms.put("format", protFormat);
    }

    private static String safeFileName(String name) {
        if (name == null || name.length() == 0)
            return "protein";
        String result = name.replaceAll("[\\\\/:*?\"<>|]+", "_");
        result = result.replaceAll("\\s+", "_");
        result = result.replaceAll("_+", "_");
        result = result.replaceAll("^[_\\.]+|[_\\.]+$", "");
        if (result.length() == 0)
            return "protein";
        return result;
    }

    private static void copyFile(File source, File destination) throws Exception {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new Exception("Could not create batch output directory: " + parent.getPath());

        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(destination);
        try {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0)
                out.write(buffer, 0, length);
        } finally {
            in.close();
            out.close();
        }
    }

    public void run() {
        String fileName = "protter_" + UUID.randomUUID().toString();

        try {
            Map<String,File> files = new LinkedHashMap<String,File>();
            List<String> warningMessages = new ArrayList<String>();
            String batchOutputDir = Config.get("batchOutputDir", "").trim();
            File persistentOutputDir = null;
            if (batchOutputDir.length() > 0) {
                persistentOutputDir = new File(batchOutputDir);
                if (!persistentOutputDir.exists() && !persistentOutputDir.mkdirs())
                    throw new Exception("Could not create batch output directory: " + persistentOutputDir.getPath());
                if (!persistentOutputDir.isDirectory())
                    throw new Exception("batchOutputDir is not a directory: " + persistentOutputDir.getPath());
                Log.info("batch individual results will be saved to: " + persistentOutputDir.getPath());
            }

            // do the individual protter plots
            int i=0;
            for (String batchProtein : batchProteins) {
                Map<String,String> protParams = new LinkedHashMap<String,String>(parms);
                if (batchProtein.contains("=")) {
                    protParams.putAll(
                            Splitter.on('&').omitEmptyStrings().trimResults().withKeyValueSeparator("=").split(batchProtein)
                    );
                } else {
                    protParams.put("up", batchProtein.trim());
                }

                i++;
                String identifier = protParams.containsKey("up") ? protParams.get("up") :
                        (protParams.containsKey("title") ? protParams.get("title") : "protein #" + i);
                Log.info("batch process " + fileName + " (" + i + "/" + batchProteins.size() + "): " + protParams);

                try {
                    ProtFile protFile = ProtProvider.getFile(protParams);
                    String extension = protParams.get("format");
                    String outputName = safeFileName(protFile.fileName);
                    if (!outputName.toLowerCase().endsWith("." + extension.toLowerCase()))
                        outputName += "." + extension;

                    // Prefix with batch position so duplicate protein names cannot overwrite one another.
                    outputName = String.format("%03d_%s", i, outputName);
                    files.put(outputName, protFile.file);

                    if (persistentOutputDir != null) {
                        File destination = new File(persistentOutputDir, outputName);
                        copyFile(protFile.file, destination);
                        Log.info("saved batch result: " + destination.getPath());
                    }
                } catch (Exception e) {
                    String reason = e.getMessage();
                    if (reason == null || reason.length() == 0)
                        reason = e.getClass().getName();
                    String skipped = "SKIPPED " + identifier + ": " + reason;
                    warningMessages.add(skipped);
                    Log.warn(skipped);
                }
                System.gc();
            }

            if (files.size()==0)
                throw new Exception("Not a single Protter image could be generated.\n\n" + Util.join(warningMessages,"\n"));

            File outputFile;
            if (batchFormat.equals("batchPdf")) {
                outputFile = new File(ProtProvider.protsDir, fileName + ".pdf");
                Log.info("merging files to: "+outputFile.getPath());
                PDFMergerUtility pdfMerger = new PDFMergerUtility();
                for (File file : files.values())
                    pdfMerger.addSource(file);
                pdfMerger.setDestinationFileName(outputFile.getPath());
                pdfMerger.mergeDocuments();
            } else {
                outputFile = new File(ProtProvider.protsDir, fileName + ".zip");
                Log.info("zipping files to: "+outputFile.getPath());
                byte[] buffer = new byte[1024];
                ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile));
                for (String zipfilename : files.keySet()) {
                    FileInputStream fin = new FileInputStream(files.get(zipfilename));
                    zout.putNextEntry(new ZipEntry(zipfilename));
                    int length;
                    while((length = fin.read(buffer)) > 0)
                        zout.write(buffer, 0, length);
                    zout.closeEntry();
                    fin.close();
                }
                zout.close();
            }

            // Keep files when batchOutputDir is configured; otherwise preserve original cleanup behavior.
            if (persistentOutputDir == null) {
                for (File file : files.values())
                    file.delete();
            } else {
                Log.info("keeping generated batch files because batchOutputDir is configured: " + persistentOutputDir.getPath());
            }

            // Also save the combined PDF/ZIP to the configured persistent directory.
            if (persistentOutputDir != null) {
                File combinedDestination = new File(persistentOutputDir, outputFile.getName());
                copyFile(outputFile, combinedDestination);
                Log.info("saved combined batch result: " + combinedDestination.getPath());
            }

            List<String> mailText = new ArrayList<String>();
            mailText.add("Hello,");
            mailText.add("Protter has finished visualizing your batch request.");
            mailText.add("Please download the collected images here:");
            mailText.add(ProtterServer.baseUrl + "/download?file=" + outputFile.getName());
            if (persistentOutputDir != null)
                mailText.add("Individual generated files were also saved to: " + persistentOutputDir.getPath());
            if (warningMessages.size()>0) mailText.add("Some proteins were skipped; see the SKIPPED messages below.");
            mailText.add("");
            mailText.add("Thank you for using Protter!");
            if (warningMessages.size()>0) {
                mailText.add("");
                mailText.add("");
                mailText.add("Skipped proteins:");
                mailText.addAll(warningMessages);
            }
            Util.sendMail(batchEmail, "Your Protter images are ready!", Util.join(mailText,"\n"));
        } catch (Exception e) {
            Log.error("an error occured during batch processing of '"+fileName+"': "+e.getMessage());
            e.printStackTrace();
            try {
                Util.sendMail(batchEmail, "Your Protter visualization job was unsuccessful", "Sorry!\nAn error occured and Protter could not visualize your batch visualization request...\nError message: "+e.getMessage());
            } catch (Exception e1) {
                Log.error("an error occured during batch processing of '"+fileName+"' - could not send notification: "+e.getMessage());
                e1.printStackTrace();
            }
        }
        System.gc();
    }

    // STATIC FUNCTIONS

    public static final int maxBatchSize = Integer.parseInt(Config.get("maxBatchSize", "0"));

    public static Response processBatchRequest(Map<String,String> parms, Socket socket) {
        try {
            String batchFormat = parms.remove("format");
            String batchEmail = parms.remove("email");
            List<String> batchProteins = Lists.newArrayList(Splitter.on('\n').omitEmptyStrings().trimResults().split(parms.remove("proteins")));
            if (batchProteins.size()==0)
                throw new Exception("No proteins specified. Please try again!");
            if (maxBatchSize>0 && batchProteins.size()>maxBatchSize) // check the limits
                throw new Exception("Batch processing is limited to "+maxBatchSize+" proteins. Try again with less proteins.");
            if (batchEmail==null || batchEmail.length()==0)
                throw new Exception("Error: no email specified. Please try again!");

            Log.info(socket.getInetAddress().getHostAddress()+" "+socket.getInetAddress().getCanonicalHostName() + " starting batch process for '"+batchEmail+"' with "+batchProteins.size()+" proteins and params: "+parms);
            BatchProcess batch = new BatchProcess(batchFormat, batchEmail, batchProteins, parms);
            batch.start();
            return new Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Your batch request is currently being processed.\n" +
                    "The results will be sent to '"+batchEmail+"'.\n\n" +
            "Thank you for using Protter!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }
}
