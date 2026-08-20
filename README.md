# Guess Market

This is a Java program for a "guess market" - a place where you can trade Yes/No
shares on events (for example "will it rain tomorrow?"). The price of each event is
worked out with the LMSR method. This is part 1 of the course project.

## How the code is split

The project has two parts:

- **engine** (`engine/src/guessmarket/engine`) - this is the logic. It keeps the
  events, does the LMSR math, reads the XML file and checks it. It does not print
  anything to the screen.
- **ui** (`ui/src/guessmarket/ui`) - this is the console part. It shows the menu, reads
  what the user types, prints the results, and asks the engine to do the work. The
  `main` method is here.

## The menu

1. Load events file
2. Display events
3. Event trading status
4. Participate in an event (buy shares)
5. Close event
6. Exit
7. Save system state (bonus)
8. Load a saved system state (bonus)

Bonus implemented: Save/Load system state. Command 7 saves everything about the
current session to a file (not just the original XML data), and command 8 reads
it back and continues from where it left off.

## How to run

You need Java 25.

Easy way (Windows): double-click `run.bat`. It compiles the code and starts the program.

Or from the command line, in the project folder:

```
javac -d engine/out engine/src/guessmarket/engine/*.java
javac -cp engine/out -d ui/out ui/src/guessmarket/ui/*.java
java -cp "engine/out;ui/out" guessmarket.ui.Main
```

## Test files

The `test-files` folder has some XML files to try:

- `single.xml`, `multiple.xml` - good files
- `error-2-duplicate-id.xml` - two events with the same id (bad file)
- `error-3-bad-commission.xml` - a commission over 90 (bad file)
