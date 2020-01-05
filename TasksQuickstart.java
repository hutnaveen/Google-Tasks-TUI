import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TasksQuickstart {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    private static final String APPLICATION_NAME = "Google Tasks API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(TasksScopes.TASKS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = TasksQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    private static List<Task> tasks;
    private static Tasks nservice;
    public static void main(String... args) throws IOException, GeneralSecurityException, ParseException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Tasks service = new Tasks.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the first 10 task lists.
        System.out.println(ANSI_GREEN+ "Ready: "+ ANSI_RESET);
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        nservice = service;
        tasks = service.tasks().list("@default").execute().getItems();
        tasks.remove(0);
        service.tasks().list("@default").execute().setItems(tasks);
        while (true) {
            if(input.contains("remove"))
            {
                deleteTask(Integer.parseInt(input.substring(input.indexOf(" ")+1)));
            }
            else if(input.contains("move"))
            {
                order(Integer.parseInt(input.substring(input.indexOf(" ")+1, input.lastIndexOf(" "))),Integer.parseInt(input.substring(input.lastIndexOf(" ")+1)));
            }
            else if ("q".equals(input)) {
                System.exit(0);
            } else if ("list".equals(input)) {
                listTasks();
            } else if ("add".equals(input)) {
                insertTask();
            }
            else
                System.out.println(ANSI_RED + "Command not recognized"+ ANSI_RESET);
            input = in.nextLine().trim();
        }

    }
    public static void listTasks() throws IOException
    {
        Calendar cal = Calendar.getInstance();
        tasks = nservice.tasks().list("@default").execute().getItems();
        //nservice.tasks().list("@default").execute().
        int i = 0;
        if(tasks==null||tasks.isEmpty())
            System.out.println("empty");
        else {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/YY");
            for (Task task : tasks) {
                cal.setTimeInMillis(task.getDue().getValue() + 86400000);
                if(task.getNotes()!= null)
                    System.out.println(ANSI_YELLOW+ i + ": " + ANSI_RESET +task.getTitle() +", "+ANSI_CYAN+ "Notes: "+task.getNotes() + ANSI_RESET+"," + ANSI_BLUE+" Due: " + format.format(cal.getTime())+ANSI_RESET);
                else
                    System.out.println(ANSI_YELLOW+ i + ": " + ANSI_RESET +task.getTitle() + ANSI_BLUE+" Due: " + format.format(cal.getTime())+ANSI_RESET);

                i++;
            }
        }
    }
    public static void insertTask() throws IOException, ParseException {
        System.out.println("title, notes, due date ex:(01/05)");
        System.out.print("-> ");
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        String[] params = input.split(", ");

        Task task = new Task();
        task.setTitle(params[0]);
        task.setNotes(params[1]);
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String dateStart = params[2]+"/2020 08:00:00";
        Date d1 = format.parse(dateStart);
        task.setDue(new DateTime(d1.getTime()));
        nservice.tasks().insert("@default", task).execute();
        listTasks();
    }
    public static void deleteTask(int index) throws IOException {
        nservice.tasks().delete("@default", tasks.get(index).getId()).execute();
        listTasks();
    }
    public static void order(int taskID, int prevID) throws IOException {
        System.out.println(taskID);
        System.out.println(prevID);
        //tasks = nservice.tasks().list("@default").execute().getItems();

        Tasks.TasksOperations.Move move = nservice.tasks().move("@default", tasks.get(taskID).getId());
        //move.setParent("parentTaskID");
        move.setPrevious(tasks.get(prevID).getId());
        move.execute();

// Print the new values.
        listTasks();
    }



}