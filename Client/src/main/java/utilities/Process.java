package utilities;

import exceptions.ServerUnavailableException;
import utilities.commands.*;
import input.*;

import java.io.IOException;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Class that is responsible for defining command and input source.
 */
public class Process {

    private HashMap<String, Command> commands = new HashMap<>();
    private Input input;
    private ConnectionManager connectionManager;

    private String login;
    private byte[] password;

    /**
     * Constructs a {@code Process} with given collection and input source.
     * Also defines all the commands available.
     *
     * @param source input stream.
     */
    public Process(Input source, ConnectionManager cm) {
        input = source;
        connectionManager = cm;
        commands.put("help", new HelpCommand(input, commands));
        commands.put("add", new AddCommand(input));
        commands.put("info", new InfoCommand(input));
        commands.put("clear", new ClearCommand(input));
        commands.put("execute_script", new ExecuteScriptCommand(input, connectionManager, this));
        commands.put("print_descending", new PrintDescendingCommand(input));
        commands.put("history", new HistoryCommand(input));
        commands.put("print_field_descending_age", new PrintFieldDescendingAgeCommand(input));
        commands.put("remove_any_by_killer", new RemoveAnyByKillerCommand(input));
        commands.put("remove_by_id", new RemoveByIdCommand(input));
        commands.put("remove_first", new RemoveFirstCommand(input));
        commands.put("remove_greater", new RemoveGreaterCommand(input));
        commands.put("show", new ShowCommand(input));
        commands.put("update", new UpdateIdCommand(input));
        commands.put("exit", new ExitCommand(input));
    }

    public Process(Input source, ConnectionManager cm, String login, byte[] password) {
        this(source, cm);
        this.login = login;
        this.password = password;
    }

    public HashMap<String, Command> getCommands() {
        return commands;
    }

    /**
     * Method defines commands from console input and executes them.
     */
    public void defineCommand() {

        authorize();

        String command;

        while (true) {

            System.out.print("Input a command: ");
            try {
                command = input.next();
            } catch (NoSuchElementException e) {
                System.exit(0);
                return;
            }

            if (!commands.containsKey(command)) {
                System.out.println("Wrong command!");
                continue;
            }

            sendAndExecute(command);
        }
    }

    private void authorize(){
        Response response = new Response();
        while (true) {
            boolean register = input.needRegistration();
            login = input.inputLogin();
            try {
                password = MessageDigest.getInstance("SHA-512").digest(input.inputPassword().getBytes());
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Algorithm error");
                System.exit(0);
            }

            Request request = new Request(login, password);
            request.setRegistering(register);

            try {
                connectionManager.connect();
            } catch (ServerUnavailableException e) {
                System.out.println("Server is temporarily unavailable");
            }

            try {
                connectionManager.send(request);
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                System.out.println("IOException happened. Unable to send data. " + e.getMessage());
                return;
            }

            try {
                response = connectionManager.receive();
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found");
            } catch (IOException e) {
                System.out.println("IOException happened while receiving data");
            }

            if (response.isSuccessfulConnect()) {
                break;
            } else {
                System.out.println(response.getMessage());
            }

        }

    }

    /**
     * Method defines commands from file input and executes them.
     */
    public void defineFileCommand() {
        String command;
        while (input.hasNext()) {
            command = input.next();

            if (commands.containsKey(command)) {
                sendAndExecute(command);
            } else {
                System.out.println("Wrong command!");
                break;
            }
        }
    }

    private void sendAndExecute(String command) {

        Command com = commands.get(command);
        if (!com.execute())
            return;

        try {
            connectionManager.connect();
        } catch (ServerUnavailableException e) {
            System.out.println("Server is temporarily unavailable");
            if (com instanceof ExitCommand) {
                System.exit(0);
            }
            return;
        }

        Request request = new Request(login, password);
        request.setCommand(com);

        try {
            connectionManager.send(request);
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            System.out.println("IOException happened. Unable to send data");
            e.printStackTrace();
            return;
        }

        try {
            Response response = connectionManager.receive();
            processResult(response);
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (IOException e) {
            System.out.println("IOException happened while receiving data");
        }
    }

    private void processResult(Response response) {
        if (response.getToExit()) {
            System.exit(0);
        } else if (response.getCollection() != null) {
            response.getCollection().forEach(System.out::println);
        } else {
            System.out.println(response.getMessage());
        }
    }

    public String getLogin() {
        return login;
    }

    public byte[] getPassword() {
        return password;
    }
}
