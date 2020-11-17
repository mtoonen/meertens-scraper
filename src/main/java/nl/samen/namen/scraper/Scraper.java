package nl.samen.namen.scraper;

import nl.samen.namen.entities.Voornaam;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scraper {

    public static void main(String[] args) throws IOException {
        Scraper s = new Scraper();
        s.scrape();
    }

    private final String BASEURL = "https://www.meertens.knaw.nl/nvb/naam/pagina%s/begintmet/%s";
    char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    public void scrape() throws IOException {
        try (
            BufferedWriter writer = Files.newBufferedWriter(Paths.get("test.csv"));
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("Name", "Mannen", "Vrouwen"));
        ) {

            for (char letter : alphabet) {
                processLetter(letter, csvPrinter);
            }

        }catch(Exception e ){
            System.err.println("Error: " + e.getLocalizedMessage());
        }
    }

    private void processLetter(char letter,CSVPrinter csvPrinter) throws IOException {
        String url = String.format(BASEURL, "1", letter);
        Document doc = Jsoup.connect(url).get();
        String title = doc.title();
        Elements descriptions = doc.getElementsByClass("description");
        Element description = descriptions.get(0);
        List<Voornaam> namen = new ArrayList<>();
        namen.addAll(processPage(doc));
        int numpages = calculateNumPages(description.text());
        for (int i = 1; i <= numpages; i++) {
            namen.addAll((processPage(String.format(BASEURL, "" + i, letter))));
            if(namen.size() > 1000){
                writeBuffer(namen, csvPrinter);
                namen.clear();
            }
        }

        writeBuffer(namen, csvPrinter);
    }

    private void writeBuffer(List<Voornaam> namen, CSVPrinter csvPrinter) throws IOException {
        namen.forEach(voornaam -> {
            try {
                csvPrinter.printRecord(voornaam.getNaam(), voornaam.getMannen(), voornaam.getVrouwen());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        csvPrinter.flush();
    }

    private int calculateNumPages(String description) {
        int startindex = description.indexOf("van") + 4;
        int endindex = description.indexOf(" ", startindex);
        String totalString = description.substring(startindex, endindex);
        int total = Integer.parseInt(totalString);

        return (int) Math.ceil(total / (double) 15.0);
    }

    private List<Voornaam> processPage(String pageurl) throws IOException {
        Document doc = Jsoup.connect(pageurl).get();
        return processPage(doc);
    }

    private List<Voornaam> processPage(Document doc) {
        Element table = doc.select("table").get(0); //select the first table.
        Elements rows = table.select("tr");
        List<Voornaam> namen = new ArrayList<>();
        rows.forEach(row -> {
            Voornaam naam = scrapeName(row);
            if (naam != null) {
                namen.add(naam);
            }
        });
        return namen;
    }

    private Voornaam scrapeName(Element row) {
        if (row.child(0).text().equals("Voornaam")) {
            return null;// header row
        }
        Voornaam naam = new Voornaam();
        naam.setNaam(row.child(0).text());
        naam.setMannen(row.child(1).text());
        naam.setVrouwen(row.child(2).text());
        return naam;
    }

}
