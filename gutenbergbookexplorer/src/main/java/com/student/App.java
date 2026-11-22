package com.student;

import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {
    private static final int MAX_BOOKS = 20;
    private static Book[] catalog = new Book[MAX_BOOKS];
    private static int catalogCount = 0;

    private static Book[] readingList = new Book[MAX_BOOKS];
    private static int readingCount = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        do {
            printMenu();
            System.out.print("Enter your choice: ");
            String input = scanner.nextLine().trim();

            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number from the menu.");
                continue;
            }

            switch (choice) {
                case 1:
                    exploreBooks();
                    break;
                case 2:
                    System.out.print("Enter title keyword to search: ");
                    String titleQuery = scanner.nextLine().trim();
                    if (titleQuery.isEmpty()) {
                        System.out.println("Title keyword cannot be empty.");
                    } else {
                        searchBook(titleQuery);
                    }
                    break;
                case 3:
                    System.out.print("Enter author keyword to search: ");
                    String authorQuery = scanner.nextLine().trim();
                    if (authorQuery.isEmpty()) {
                        System.out.println("Author keyword cannot be empty.");
                    } else {
                        searchBook(authorQuery, true);
                    }
                    break;
                case 4:
                    addBook(scanner);
                    break;
                case 5:
                    viewReadingList();
                    break;
                case 6:
                    removeBook(scanner);
                    break;
                case 7:
                    displayBookDetails(scanner);
                    break;
                case 0:
                    System.out.println("Exiting Gutenberg Book Explorer. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please select a valid menu option.");
                    break;
            }
        } while (choice != 0);

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\nGUTENBERG BOOK EXPLORER");
        System.out.println("1. Explore Books");
        System.out.println("2. Search by Title");
        System.out.println("3. Search by Author");
        System.out.println("4. Add Book to Reading List");
        System.out.println("5. View Reading List");
        System.out.println("6. Remove Book");
        System.out.println("7. Display Book Details");
        System.out.println("0. Exit");
    }

    static void exploreBooks() {
        catalogCount = 0;
        try {
            Document doc = Jsoup.connect("https://www.gutenberg.org/browse/scores/top")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10000)
                    .get();

            Elements links = doc.select("a[href^=/ebooks/]");

            for (Element link : links) {
                if (catalogCount >= 10) {
                    break;
                }

                String href = link.attr("href").trim();
                if (!href.matches("/ebooks/\\d+")) {
                    continue;
                }

                String rawText = link.text().trim();
                if (rawText.isEmpty() || rawText.toLowerCase().startsWith("top ")) {
                    continue;
                }

                String url = link.absUrl("href");
                if (isDuplicateUrl(catalog, catalogCount, url)) {
                    continue;
                }

                String title = rawText;
                String author = extractAuthor(link, rawText);

                catalog[catalogCount] = new Book(title, author, url);
                catalogCount++;
            }

            if (catalogCount == 0) {
                System.out.println("No books found on Project Gutenberg right now.");
                return;
            }

            System.out.println("\n--- Explored Gutenberg Books ---");
            for (int i = 0; i < catalogCount; i++) {
                catalog[i].displaySummary(i + 1);
            }
        } catch (Exception e) {
            System.out.println("Connection error while exploring books: " + e.getMessage());
        }
    }

    private static String extractAuthor(Element link, String rawText) {
        if (rawText.contains(" by ")) {
            int idx = rawText.lastIndexOf(" by ");
            String possibleAuthor = rawText.substring(idx + 4).trim();
            if (!possibleAuthor.isEmpty()) {
                return possibleAuthor;
            }
        }
        Element parent = link.parent();
        if (parent != null) {
            String parentText = parent.text().trim();
            if (parentText.contains(" by ")) {
                int idx = parentText.lastIndexOf(" by ");
                String possibleAuthor = parentText.substring(idx + 4).trim();
                if (!possibleAuthor.isEmpty()) {
                    return possibleAuthor;
                }
            }
        }
        return "Unknown Author";
    }

    private static boolean isDuplicateUrl(Book[] list, int count, String url) {
        for (int i = 0; i < count; i++) {
            if (list[i] != null && list[i].getUrl().equalsIgnoreCase(url)) {
                return true;
            }
        }
        return false;
    }

    static void searchBook(String title) {
        searchBook(title, false);
    }

    static void searchBook(String keyword, boolean searchByAuthor) {
        String lowerKeyword = keyword.toLowerCase();
        boolean found = false;

        if (catalogCount > 0) {
            for (int i = 0; i < catalogCount; i++) {
                Book b = catalog[i];
                boolean matches = searchByAuthor
                        ? b.getAuthor().toLowerCase().contains(lowerKeyword)
                        : b.getTitle().toLowerCase().contains(lowerKeyword);
                if (matches) {
                    if (!found) {
                        System.out.println("\n--- Search Results (Explored Books) ---");
                        found = true;
                    }
                    b.displaySummary(i + 1);
                }
            }
        }

        if (!found) {
            for (int i = 0; i < readingCount; i++) {
                Book b = readingList[i];
                boolean matches = searchByAuthor
                        ? b.getAuthor().toLowerCase().contains(lowerKeyword)
                        : b.getTitle().toLowerCase().contains(lowerKeyword);
                if (matches) {
                    if (!found) {
                        System.out.println("\n--- Search Results (Reading List) ---");
                        found = true;
                    }
                    b.displaySummary(i + 1);
                }
            }
        }

        if (!found) {
            System.out.println("No books matched \"" + keyword + "\".");
        }
    }

    static void addBook(Scanner scanner) {
        if (readingCount >= readingList.length) {
            System.out.println("Reading list is full (maximum " + readingList.length + " books). Remove a book first.");
            return;
        }

        if (catalogCount == 0) {
            System.out.println("No explored books available yet. Run option 1 (Explore Books) first.");
            return;
        }

        System.out.println("\n--- Select a Book to Add ---");
        for (int i = 0; i < catalogCount; i++) {
            catalog[i].displaySummary(i + 1);
        }

        System.out.print("Enter book number (1-" + catalogCount + "): ");
        String input = scanner.nextLine().trim();
        int selection;
        try {
            selection = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        if (selection < 1 || selection > catalogCount) {
            System.out.println("Book number out of range.");
            return;
        }

        Book selected = catalog[selection - 1];
        if (isDuplicateUrl(readingList, readingCount, selected.getUrl())) {
            System.out.println("That book is already in your reading list.");
            return;
        }

        readingList[readingCount] = selected;
        readingCount++;
        System.out.println("Added \"" + selected.getTitle() + "\" to your reading list (" + readingCount + "/" + readingList.length + ").");
    }

    static void viewReadingList() {
        if (readingCount == 0) {
            System.out.println("Your reading list is currently empty.");
            return;
        }

        System.out.println("\n--- Personal Reading List (" + readingCount + "/" + readingList.length + ") ---");
        for (int i = 0; i < readingCount; i++) {
            readingList[i].displaySummary(i + 1);
        }
    }

    static void removeBook(Scanner scanner) {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty. Nothing to remove.");
            return;
        }

        viewReadingList();
        System.out.print("Enter the number of the book to remove (1-" + readingCount + "): ");
        String input = scanner.nextLine().trim();
        int selection;
        try {
            selection = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        if (selection < 1 || selection > readingCount) {
            System.out.println("Selection out of range.");
            return;
        }

        int removeIndex = selection - 1;
        String removedTitle = readingList[removeIndex].getTitle();

        for (int i = removeIndex; i < readingCount - 1; i++) {
            readingList[i] = readingList[i + 1];
        }
        readingList[readingCount - 1] = null;
        readingCount--;

        System.out.println("Removed \"" + removedTitle + "\" from your reading list.");
    }

    static void displayBookDetails(Scanner scanner) {
        if (readingCount > 0) {
            viewReadingList();
            System.out.print("Enter reading list number for details (or 0 to check explored books): ");
            String input = scanner.nextLine().trim();
            int selection;
            try {
                selection = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
                return;
            }

            if (selection > 0) {
                if (selection <= readingCount) {
                    readingList[selection - 1].displayDetails();
                } else {
                    System.out.println("Selection out of range.");
                }
                return;
            }
        }

        if (catalogCount == 0) {
            System.out.println("No books available to display details for. Explore books or add to reading list first.");
            return;
        }

        System.out.println("\n--- Explored Books ---");
        for (int i = 0; i < catalogCount; i++) {
            catalog[i].displaySummary(i + 1);
        }
        System.out.print("Enter explored book number for details (1-" + catalogCount + "): ");
        String input = scanner.nextLine().trim();
        int selection;
        try {
            selection = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        if (selection < 1 || selection > catalogCount) {
            System.out.println("Selection out of range.");
            return;
        }

        catalog[selection - 1].displayDetails();
    }
}