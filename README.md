# Guess Market

A console-based prediction market, written in Java. Users load events from an XML
file and trade "Yes/No" shares on each event using the LMSR (Logarithmic Market
Scoring Rule) pricing method. This is Part 1 of a rolling course project.

## Project structure

The project is split into two modules:

- **engine** (`engine/src/guessmarket/engine`) - the logic of the system. It holds
  the events, does the LMSR math, loads and validates the XML file, and answers
  requests. It does no console input/output.
- **ui** (`ui/src/guessmarket/ui`) - the console interface. It shows the menu, reads
  the user input, prints the results, and asks the engine to do the work. This is the
  only module that prints to the screen or reads from the keyboard, and it holds the
  `main` method.

## Menu commands

1. Load events file (XML)
2. Display events
3. Event trading status
4. Participate in an event (buy shares)
5. Close event
6. Exit

## How to build and run

Requires Java 25.

Easiest way (Windows): double-click `run.bat`. It compiles the source and starts the
program.

Or manually from the project folder:

```
javac -d engine/out engine/src/guessmarket/engine/*.java
javac -cp engine/out -d ui/out ui/src/guessmarket/ui/*.java
java -cp "engine/out;ui/out" guessmarket.ui.Main
```

## Test files

The `test-files` folder holds sample XML files to load:

- `single.xml`, `multiple.xml` - valid files
- `error-2-duplicate-id.xml` - two events share the same id (invalid)
- `error-3-bad-commission.xml` - a commission value above 90 (invalid)
