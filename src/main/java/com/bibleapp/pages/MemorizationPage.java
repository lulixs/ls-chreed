package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.List;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import com.bibleapp.difficulty.VerseDifficultyServer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MemorizationPage extends VBox {

private static final String[] DIFFICULTY_LABELS = {
"Copy-down",
"Every-other A",
"Every-other B",
"Full-memory"
};

private static final String[] BIBLE_BOOK_ORDER = {
"Genesis","Exodus","Leviticus","Numbers","Deuteronomy",
"Joshua","Judges","Ruth","1 Samuel","2 Samuel",
"1 Kings","2 Kings","1 Chronicles","2 Chronicles",
"Ezra","Nehemiah","Esther","Job","Psalms","Proverbs",
"Ecclesiastes","Song of Songs","Isaiah","Jeremiah",
"Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos",
"Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah",
"Haggai","Zechariah","Malachi","Matthew","Mark","Luke",
"John","Acts","Romans","1 Corinthians","2 Corinthians",
"Galatians","Ephesians","Philippians","Colossians",
"1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy",
"Titus","Philemon","Hebrews","James","1 Peter","2 Peter",
"1 John","2 John","3 John","Jude","Revelation"
};

private static final Map<String, Map<Integer, Integer>> BIBLE = loadBibleStructure();

private static Map<String, Map<Integer, Integer>> loadBibleStructure() {
try (InputStream is = openBibleStructureStream()) {
if (is == null) {
throw new IOException("BibleStructure.json was not found on the classpath or in project resources.");
}

JSONParser parser = new JSONParser();
JSONObject root = (JSONObject) parser.parse(new InputStreamReader(is, StandardCharsets.UTF_8));
Map<String, Map<Integer, Integer>> bible = new HashMap<>();

for (Object bookKey : root.keySet()) {
String book = (String) bookKey;
JSONObject chapters = (JSONObject) root.get(book);

Map<Integer, Integer> chapterMap = new HashMap<>();

for (Object chapterKey : chapters.keySet()) {
int chapter = Integer.parseInt((String) chapterKey);
int verses = ((Long) chapters.get(chapterKey)).intValue();
chapterMap.put(chapter, verses);
}

bible.put(book, chapterMap);
}

return bible;
} catch (Exception e) {
throw new IllegalStateException("Failed to load Bible structure", e);
}
}

private static InputStream openBibleStructureStream() throws IOException {
InputStream classResource = MemorizationPage.class.getResourceAsStream("/com/bibleapp/BibleStructure.json");
if (classResource != null) {
return classResource;
}

ClassLoader classLoader = MemorizationPage.class.getClassLoader();
if (classLoader != null) {
InputStream loaderResource = classLoader.getResourceAsStream("com/bibleapp/BibleStructure.json");
if (loaderResource != null) {
return loaderResource;
}
}

Path[] fallbackPaths = new Path[] {
Path.of("src", "main", "resources", "com", "bibleapp", "BibleStructure.json"),
Path.of("resources", "com", "bibleapp", "BibleStructure.json"),
Path.of("target", "classes", "com", "bibleapp", "BibleStructure.json")
};

for (Path path : fallbackPaths) {
if (Files.exists(path)) {
return Files.newInputStream(path);
}
}

return null;
}

private final VBox leftScrollContent;
private final StackPane appRoot;
private Runnable currentClosePopupHandler;
private VerseDifficultyServer difficulty;

private static final String[] DIFFICULTY_COLORS = {
"#1D9E75", "#378ADD", "#BA7517", "#D85A30", "#555555"
};

private int currentDifficulty = 0;
private Button prevBtn, nextBtn;
private Label diffNameLabel;
private Label diffLockedLabel;
private VBox rightColumn;
private MemorizedVerse currentVerse;
private final Rectangle[] pips = new Rectangle[4];

private List<Label> wordLabels = new ArrayList<>();
private int currentWordIndex = 0;
private boolean[] wordCorrect;

public MemorizationPage(StackPane appRoot) {
this.appRoot = appRoot;
getStyleClass().add("page");
setSpacing(20);

difficulty = new VerseDifficultyServer();

Label title = new Label("Memorize");
title.getStyleClass().add("page-title");

HBox columnsContainer = new HBox(20);
columnsContainer.setAlignment(Pos.TOP_LEFT);
HBox.setHgrow(columnsContainer, Priority.ALWAYS);
VBox.setVgrow(columnsContainer, Priority.ALWAYS);

VBox leftColumn = new VBox(10);
leftColumn.getStyleClass().add("memorize-left-column");
leftColumn.setPrefWidth(200);
leftColumn.setMinWidth(150);

Label leftLabel = new Label("My Verses");
leftLabel.getStyleClass().add("column-header");
leftColumn.getChildren().add(leftLabel);

leftScrollContent = new VBox(10);
leftScrollContent.getStyleClass().add("memorize-scroll-content");
leftScrollContent.setPadding(new Insets(10));

ScrollPane leftScrollPane = new ScrollPane(leftScrollContent);
leftScrollPane.getStyleClass().add("memorize-scroll");
leftScrollPane.setFitToWidth(true);
leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
VBox.setVgrow(leftScrollPane, Priority.ALWAYS);
leftColumn.getChildren().add(leftScrollPane);

Button addButton = new Button("+ Add Verse");
addButton.getStyleClass().add("add-verse-btn");
addButton.setOnAction(e -> showAddVersePopup());

HBox buttonContainer = new HBox(addButton);
buttonContainer.setAlignment(Pos.CENTER);
leftColumn.getChildren().add(buttonContainer);

rightColumn = new VBox(10);
rightColumn.getStyleClass().add("memorize-right-column");
rightColumn.setPadding(new Insets(10));

HBox.setHgrow(rightColumn, Priority.ALWAYS);
HBox.setHgrow(leftColumn, Priority.NEVER);

columnsContainer.getChildren().addAll(leftColumn, rightColumn);
VBox.setVgrow(columnsContainer, Priority.ALWAYS);

getChildren().addAll(title, columnsContainer);

loadVerseList();
showDifficultySelector();
}

private MemorizedVerse getSelectedVerse() {
return currentVerse;
}

private HBox buildDifficultySelector() {
VBox pipColumn = new VBox(6);
pipColumn.setAlignment(Pos.BOTTOM_CENTER);
pipColumn.getStyleClass().add("difficulty-pip-column");

for (int i = 3; i >= 0; i--) {
Rectangle pip = new Rectangle(18, 18);
pip.setArcWidth(6);
pip.setArcHeight(6);

pips[i] = pip;

StackPane pipWrapper = new StackPane(pip);
pipWrapper.setMinSize(18, 18);
pipWrapper.setMaxSize(18, 18);
pipColumn.getChildren().add(pipWrapper);
}

prevBtn = new Button("\u2190");
prevBtn.getStyleClass().add("diff-arrow-btn");
prevBtn.setOnAction(e -> cycleLeft());

nextBtn = new Button("\u2192");
nextBtn.getStyleClass().add("diff-arrow-btn");
nextBtn.setOnAction(e -> cycleRight());

diffNameLabel = new Label();
diffNameLabel.getStyleClass().add("diff-name-label");

diffLockedLabel = new Label();
diffLockedLabel.getStyleClass().add("diff-locked-label");

VBox textStack = new VBox(2, diffNameLabel, diffLockedLabel);
textStack.setAlignment(Pos.CENTER);
HBox.setHgrow(textStack, Priority.ALWAYS);

HBox selectorBar = new HBox(10, prevBtn, textStack, nextBtn);
selectorBar.setAlignment(Pos.CENTER);
selectorBar.getStyleClass().add("diff-selector-bar");
HBox.setHgrow(selectorBar, Priority.ALWAYS);

Label selectedVerseLabel = new Label(
currentVerse == null ? "Select a verse" : currentVerse.getReference()
);
selectedVerseLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333333;");
selectedVerseLabel.setWrapText(true);
selectedVerseLabel.setAlignment(Pos.CENTER);
selectedVerseLabel.setMaxWidth(Double.MAX_VALUE);

Label sectionLabel = new Label("Difficulty");
sectionLabel.getStyleClass().add("diff-section-label");

Button startBtn = new Button("Start");
startBtn.getStyleClass().add("add-verse-btn");
startBtn.setDisable(currentVerse == null || currentVerse.getText() == null || currentVerse.getText().trim().isEmpty());

startBtn.setOnAction(e -> {
if (currentVerse != null && currentVerse.getText() != null && !currentVerse.getText().trim().isEmpty()) {
startTask();
}
});

if (currentVerse != null) {
difficulty.setDifficulty(currentVerse, currentDifficulty);
}

VBox selectorColumn = new VBox(8, selectedVerseLabel, sectionLabel, selectorBar, startBtn);
selectorColumn.setAlignment(Pos.CENTER);
HBox.setHgrow(selectorColumn, Priority.ALWAYS);

HBox row = new HBox(16, selectorColumn, pipColumn);
row.setAlignment(Pos.BOTTOM_LEFT);
row.getStyleClass().add("diff-selector-row");

if (currentVerse != null) {
refreshDifficultyUI();
} else {
prevBtn.setDisable(true);
nextBtn.setDisable(true);
diffNameLabel.setText("No verse selected");
diffLockedLabel.setText("");
}

return row;
}

private void refreshDifficultyUI() {
if (currentVerse == null) {
return;
}

for (int i = 0; i < 4; i++) {
pips[i].setWidth(18);
pips[i].setHeight(18);

String color;

if (i < currentVerse.getNextDifficulty()) {
color = DIFFICULTY_COLORS[i];
} else {
color = DIFFICULTY_COLORS[4];

if (i != currentVerse.getNextDifficulty()) {
pips[i].setWidth(9);
pips[i].setHeight(9);
}
}

pips[i].setFill(javafx.scene.paint.Color.web(color));

if (i == currentDifficulty) {
pips[i].setStroke(javafx.scene.paint.Color.web("#000000"));
pips[i].setStrokeWidth(2.5);
} else {
pips[i].setStroke(javafx.scene.paint.Color.TRANSPARENT);
pips[i].setStrokeWidth(2.5);
}
}

diffNameLabel.setText(MemorizedVerse.getDifficultyLabel(currentDifficulty));
diffLockedLabel.setText(currentVerse.getNextDifficulty() == currentDifficulty ? "Incomplete" : "Completed");

prevBtn.setDisable(currentDifficulty == 0);
nextBtn.setDisable(currentDifficulty == Math.min(currentVerse.getNextDifficulty(), 3));
}

private void startTask() {
if (currentVerse == null || currentVerse.getText() == null || currentVerse.getText().trim().isEmpty()) {
showDifficultySelector();
return;
}

rightColumn.getChildren().clear();

currentVerse = getSelectedVerse();
difficulty.setDifficulty(currentVerse, currentDifficulty);

Button backBtn = new Button("\u2190 Back");
backBtn.getStyleClass().add("back-btn");
backBtn.setOnAction(e -> showDifficultySelector());

HBox topBar = new HBox(backBtn);
topBar.setAlignment(Pos.TOP_LEFT);

wordLabels.clear();
currentWordIndex = 0;

TextFlow verseFlow = new TextFlow();
verseFlow.setLineSpacing(6);
verseFlow.getStyleClass().add("verse-text-flow");

String[] words = difficulty.getDisplayVerse();

for (int i = 0; i < words.length; i++) {
Label wordLabel = new Label(words[i]);
wordLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
wordLabels.add(wordLabel);
verseFlow.getChildren().add(wordLabel);

if (i < words.length - 1) {
Label space = new Label(" ");
space.setStyle("-fx-font-size: 16px;");
verseFlow.getChildren().add(space);
}
}

wordCorrect = new boolean[words.length];

TextField inputField = new TextField();
inputField.getStyleClass().add("verse-input");
inputField.setPromptText("Type the first letter of each word...");

inputField.textProperty().addListener((obs, oldVal, newVal) -> {
if (!newVal.isEmpty()) {
if (!advanceWord(inputField)) {
onVerseComplete();
}
}
});

Region spacer = new Region();
VBox.setVgrow(spacer, Priority.ALWAYS);

rightColumn.getChildren().addAll(topBar, verseFlow, spacer, inputField);

highlightWord(0);

javafx.application.Platform.runLater(inputField::requestFocus);
}

private void highlightWord(int index) {
for (int i = 0; i < wordLabels.size(); i++) {
Label lbl = wordLabels.get(i);

if (i < index) {
lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #888888;");
} else if (i == index) {
lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-background-color: #FFF3CD; -fx-background-radius: 3px; -fx-padding: 1 3 1 3;");
} else {
lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
}
}
}

private boolean advanceWord(TextField inputField) {
String[] key = difficulty.getAnswerKey();

String typed = inputField.getText();

if (!typed.isEmpty()) {
wordCorrect[currentWordIndex] =
Character.toLowerCase(typed.charAt(0)) == Character.toLowerCase(key[currentWordIndex].charAt(0));
}

inputField.clear();
currentWordIndex++;

if (currentWordIndex < wordLabels.size()) {
highlightWord(currentWordIndex);
return true;
} else {
return false;
}
}

private void onVerseComplete() {
rightColumn.getChildren().clear();

String[] key = difficulty.getAnswerKey();

Button backBtn = new Button("\u2190 Back");
backBtn.getStyleClass().add("back-btn");
backBtn.setOnAction(e -> showDifficultySelector());

HBox topBar = new HBox(backBtn);
topBar.setAlignment(Pos.TOP_LEFT);

Region spacer = new Region();
VBox.setVgrow(spacer, Priority.ALWAYS);

Region spacerBottom = new Region();
VBox.setVgrow(spacerBottom, Priority.ALWAYS);

Label scoreTitle = new Label("Complete!");
scoreTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333333;");

int numCorrect = countCorrect(wordCorrect);

Label scoreLabel = new Label(numCorrect + "/" + key.length);
scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(numCorrect, key.length) + ";");

Label scoreSubtitle = new Label(getScoreMessage(numCorrect, key.length));
scoreSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");

VBox scoreBox = new VBox(8, scoreTitle, scoreLabel, scoreSubtitle);
scoreBox.setAlignment(Pos.CENTER);

TextFlow verseReview = new TextFlow();
verseReview.setLineSpacing(6);

for (int i = 0; i < key.length; i++) {
Label wordLabel = new Label(key[i]);

String color = wordCorrect[i] ? "#1D9E75" : "#D85A30";
wordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
verseReview.getChildren().add(wordLabel);

if (i < key.length - 1) {
Label space = new Label(" ");
space.setStyle("-fx-font-size: 16px;");
verseReview.getChildren().add(space);
}
}

Button actionBtn;

if (numCorrect == key.length) {
actionBtn = new Button("Continue");
actionBtn.setOnAction(e -> showDifficultySelector());

if (currentDifficulty == currentVerse.getNextDifficulty()) {
unlockNextDifficulty(currentVerse);

if (currentDifficulty < 3) {
currentDifficulty++;
}
}
} else {
actionBtn = new Button("Retry");
actionBtn.setOnAction(e -> startTask());
}

actionBtn.getStyleClass().add("add-verse-btn");

HBox actionBar = new HBox(actionBtn);
actionBar.setAlignment(Pos.CENTER);

rightColumn.getChildren().addAll(topBar, spacer, scoreBox, verseReview, spacerBottom, actionBar);
}

private int countCorrect(boolean[] arr) {
int count = 0;

for (int i = 0; i < arr.length; i++) {
if (arr[i]) {
count++;
}
}

return count;
}

private String getScoreColor(int score, int total) {
if (score >= total) return "#1D9E75";
if (score * 2 >= total) return "#BA7517";
return "#D85A30";
}

private String getScoreMessage(int score, int total) {
if (score >= total) return "Great work!";
if (score * 2 >= total) return "Keep practicing!";
return "Don't give up!";
}

private void showDifficultySelector() {
rightColumn.getChildren().clear();

Region spacer = new Region();
VBox.setVgrow(spacer, Priority.ALWAYS);

rightColumn.getChildren().addAll(spacer, buildDifficultySelector());
}

private void cycleLeft() {
if (currentVerse != null && currentDifficulty > 0) {
currentDifficulty--;
refreshDifficultyUI();
}
}

private void cycleRight() {
if (currentVerse != null && currentDifficulty < Math.min(currentVerse.getNextDifficulty(), 3)) {
currentDifficulty++;
refreshDifficultyUI();
}
}

public void unlockNextDifficulty(MemorizedVerse verse) {
if (verse.getNextDifficulty() < 4) {
verse.setNextDifficulty(verse.getNextDifficulty() + 1);
}

refreshDifficultyUI();
}

private void loadVerseList() {
leftScrollContent.getChildren().clear();

List<MemorizedVerse> verses = DataStore.getMemorizationList();

if (verses.isEmpty()) {
currentVerse = null;

Label empty = new Label("No verses yet. Tap '+ Add Verse'.");
empty.getStyleClass().add("popup-placeholder");
leftScrollContent.getChildren().add(empty);
} else {
boolean selectedVerseStillExists = false;

if (currentVerse != null) {
for (MemorizedVerse verse : verses) {
if (verse.getId().equals(currentVerse.getId())) {
currentVerse = verse;
selectedVerseStillExists = true;
break;
}
}
}

if (currentVerse == null || !selectedVerseStillExists) {
currentVerse = verses.get(0);
}

currentDifficulty = Math.min(currentVerse.getNextDifficulty(), 3);

for (MemorizedVerse verse : verses) {
leftScrollContent.getChildren().add(buildVerseCard(verse));
}
}
}

private VBox buildVerseCard(MemorizedVerse verse) {
VBox card = new VBox(4);
card.getStyleClass().add("verse-card");
card.setPadding(new Insets(8));
card.setCursor(Cursor.HAND);

if (currentVerse != null && verse.getId().equals(currentVerse.getId())) {
card.setStyle("-fx-border-color: #378ADD; -fx-border-width: 2px; -fx-border-radius: 6px;");
}

Label refLabel = new Label(verse.getReference());
refLabel.getStyleClass().add("verse-card-reference");
refLabel.setWrapText(true);

String preview = verse.getText();

if (preview == null || preview.trim().isEmpty()) {
preview = "Verse text unavailable.";
} else if (preview.length() > 60) {
preview = preview.substring(0, 60).stripTrailing() + "…";
}

Label previewLabel = new Label(preview);
previewLabel.getStyleClass().add("verse-card-preview");
previewLabel.setWrapText(true);

ComboBox<String> diffBox = new ComboBox<>();
diffBox.getItems().addAll(DIFFICULTY_LABELS);
diffBox.setMaxWidth(Double.MAX_VALUE);

int diffIndex = Math.max(0, Math.min(verse.getNextDifficulty(), DIFFICULTY_LABELS.length - 1));
diffBox.getSelectionModel().select(diffIndex);

diffBox.setOnAction(e -> {
int selectedIndex = diffBox.getSelectionModel().getSelectedIndex();
DataStore.updateVerseDifficulty(verse.getId(), selectedIndex + 1);

if (currentVerse != null && verse.getId().equals(currentVerse.getId())) {
verse.setNextDifficulty(selectedIndex + 1);
currentVerse = verse;
currentDifficulty = Math.min(currentVerse.getNextDifficulty(), 3);
showDifficultySelector();
}

e.consume();
});

Button removeBtn = new Button("Remove");
removeBtn.getStyleClass().add("remove-verse-btn");

removeBtn.setOnAction(e -> {
DataStore.removeVerse(verse.getId());

if (currentVerse != null && verse.getId().equals(currentVerse.getId())) {
currentVerse = null;
}

loadVerseList();
showDifficultySelector();
e.consume();
});

card.setOnMouseClicked(e -> {
currentVerse = verse;
currentDifficulty = Math.min(currentVerse.getNextDifficulty(), 3);
loadVerseList();
showDifficultySelector();
});

card.getChildren().addAll(refLabel, previewLabel, diffBox, removeBtn);

return card;
}

private void showAddVersePopup() {
StackPane popupOverlay = new StackPane();
popupOverlay.getStyleClass().add("popup-overlay");
popupOverlay.setOnMouseClicked(e -> closePopup());

VBox popupContainer = new VBox(8);
popupContainer.getStyleClass().add("memorize-popup");
popupContainer.setPadding(new Insets(10));
popupContainer.setMaxWidth(380);
popupContainer.setMinWidth(320);
popupContainer.setMaxHeight(220);
popupContainer.setPrefHeight(200);

Label title = new Label("Add Verse");

Button closeBtn = new Button("X");
closeBtn.setOnAction(e -> closePopup());

HBox header = new HBox(title, closeBtn);
header.setAlignment(Pos.CENTER_LEFT);

ComboBox<String> bookBox = new ComboBox<>();

for (String book : BIBLE_BOOK_ORDER) {
if (BIBLE.containsKey(book)) {
bookBox.getItems().add(book);
}
}

bookBox.setPromptText("Book");

Spinner<Integer> chapterSpinner = new Spinner<>(1, 150, 1);
Spinner<Integer> verseSpinner = new Spinner<>(1, 200, 1);

chapterSpinner.setPrefWidth(90);
verseSpinner.setPrefWidth(90);

Label error = new Label();
error.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");

Button saveBtn = new Button("Save");

saveBtn.setOnAction(e -> {
error.setText("");

String book = bookBox.getValue();
int chapter = chapterSpinner.getValue();
int verse = verseSpinner.getValue();

if (book == null) {
error.setText("Select a book.");
return;
}

Map<Integer, Integer> chapters = BIBLE.get(book);

if (chapters == null || !chapters.containsKey(chapter)) {
error.setText("Invalid chapter for " + book);
return;
}

int maxVerse = chapters.get(chapter);

if (verse < 1 || verse > maxVerse) {
error.setText("Chapter " + chapter + " has 1-" + maxVerse + " verses.");
return;
}

saveBtn.setDisable(true);
saveBtn.setText("Loading...");

String verseText = fetchVerseTextFromApi(book, chapter, verse);

if (verseText == null || verseText.trim().isEmpty()) {
saveBtn.setDisable(false);
saveBtn.setText("Save");
error.setText("Could not load verse text from API.");
return;
}

MemorizedVerse verseObj = new MemorizedVerse(book, chapter, verse, verseText, 0);

DataStore.addVerse(verseObj);

currentVerse = verseObj;
currentDifficulty = 0;

loadVerseList();
showDifficultySelector();
closePopup();
});

VBox content = new VBox(8,
bookBox,
new HBox(8, new Label("Ch"), chapterSpinner, new Label("V"), verseSpinner),
error,
saveBtn
);

popupContainer.getChildren().addAll(header, content);

StackPane wrapper = new StackPane(popupContainer);
wrapper.setAlignment(Pos.CENTER);
wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

appRoot.getChildren().addAll(popupOverlay, wrapper);

currentClosePopupHandler = () ->
appRoot.getChildren().removeAll(popupOverlay, wrapper);
}

private String fetchVerseTextFromApi(String book, int chapter, int verse) {
try {
String reference = book + " " + chapter + ":" + verse;
String encodedReference = URLEncoder.encode(reference, StandardCharsets.UTF_8).replace("+", "%20");

String apiUrl = "https://bible-api.com/" + encodedReference;

HttpClient client = HttpClient.newBuilder()
.connectTimeout(Duration.ofSeconds(10))
.build();

HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create(apiUrl))
.timeout(Duration.ofSeconds(10))
.GET()
.build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() != 200) {
return null;
}

JSONParser parser = new JSONParser();
JSONObject json = (JSONObject) parser.parse(response.body());

Object textObj = json.get("text");

if (textObj == null) {
return null;
}

return textObj.toString().replace("\n", " ").trim();

} catch (Exception e) {
e.printStackTrace();
return null;
}
}

private void closePopup() {
if (currentClosePopupHandler != null) {
currentClosePopupHandler.run();
currentClosePopupHandler = null;
}
}
}
