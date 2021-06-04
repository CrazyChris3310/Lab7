package input;

import exceptions.WrongInputFormatException;
import utilities.Func;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Class {@code ConsoleInput} defines methods to work with an input from console.
 * Each method has a validation of User's input.
 */
public class ConsoleInput extends Input{

    /**
     * Constructs input class with console input as a source.
     */
    public ConsoleInput() {
        sc = new Scanner(System.in);
    }

    @Override
    public <T> T input(String question, String errorMessage, Func<T> rule) {
        T result;
        while (true) {
            System.out.print(question);
            String data;
            try {
                data = sc.nextLine();
                result = rule.func(data);
            } catch (NoSuchElementException e) {
                System.exit(0);
                continue;
            } catch (NumberFormatException | WrongInputFormatException e) {
                System.out.println(errorMessage);
                continue;
            }
            return result;
        }
    }


}
