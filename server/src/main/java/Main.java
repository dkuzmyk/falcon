import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static spark.Spark.*;



public class Main {

    public static void main(String[] args) {
        // 2d listArray to store the xlsx data
        List<List<String>> inventoryData = new ArrayList<>();
        List<List<String>>  distributorsData = new ArrayList<>();

        List<List<String>> restock = new ArrayList<>();

        // paths to xlsx
        String dis_path = "./resources/Distributors.xlsx";
        String inv_path = "./resources/Inventory.xlsx";

        // get xlsx data into listArray
        distributorsData = readExcel(dis_path);
        // clean garbage data in the list
        distributorsData = cleanDistributors(distributorsData);
        //printList(distributorsData); // debug


        inventoryData = readExcel(inv_path);
        //printList(inventoryData); // debug

        // find the items that need to be re-ordered
        restock = findRestockItems(inventoryData);
        //printList(restock); // debug

        // convert listArray to json
        JSONArray restockJson = arrayToJson(restock);


        //This is required to allow GET and POST requests with the header 'content-type'
        options("/*",
                (request, response) -> {
                        response.header("Access-Control-Allow-Headers",
                                "content-type");

                        response.header("Access-Control-Allow-Methods",
                                "GET, POST");


                    return "OK";
                });

        //This is required to allow the React app to communicate with this API
        before((request, response) -> response.header("Access-Control-Allow-Origin", "http://localhost:3000"));

        //TODO: Return JSON containing the candies for which the stock is less than 25% of it's capacity
        get("/low-stock", (request, response) -> {
            System.out.println("Sent Low Stock json");
            return restockJson;
        });

        //TODO: Return JSON containing the total cost of restocking candy
        List<List<String>> finalDistributorsData = distributorsData;
        post("/restock-cost", (request, response) -> {
            return managePost(request.body(), finalDistributorsData);
        });

    }
    // read the excel file and export the listArray of its content
    public static List<List<String>> readExcel(String path){
        File distributors = new File(path);
        // store the 2d array
        List<List<String>> array = new ArrayList<>();
        try {
            // create objects from the two excel files
            FileInputStream dis = new FileInputStream(distributors);

            // create workbooks to read
            XSSFWorkbook disWorkbook = new XSSFWorkbook(dis);

            // iterate thro all sheets in the excel file
            for (int i = 0; i < disWorkbook.getNumberOfSheets(); i++) {
                XSSFSheet disBook = disWorkbook.getSheetAt(i);

                Iterator<Row> rowIterator = disBook.iterator();
                // iterate thro all rows in the sheet
                while (rowIterator.hasNext()) {
                    List<String> str = new ArrayList<>();
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    // iterate thro all cells in each row
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        if(!cell.toString().equals("")) {
                            str.add(cell.toString());// not adding comma
                        }
                    }
                    array.add(str);
                }
            }

            disWorkbook.close();
            //invWorkbook.close();
        }catch(Exception e){
            System.out.println(e);
        }
        // remove column names from the data or empty strings
        for (List<String> e : array) {
            e.removeAll(Arrays.asList("", null));
        }

        return array;
    }

    // debug print listarray
    public static void printList(List<List<String>> l){
        for (List<String> e : l){
            for (String s : e){
                System.out.print(s);
            }
            System.out.println();
        }
    }

    public static JSONArray managePost(String body, List<List<String>> dis){
        double totalCost = 0.0;

        // round floating numbers to 2 decimals
        // using Math.round for simplicity

        // create a json array out of the string received from request
        JSONArray arr = new JSONArray(body);
        // creating 2d array with two columns: ID and quantity of items (value)
        String[][] data = new String[arr.length()][2];
        // modelling the string into an array
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            System.out.print("Received Object "+i+" ");
            System.out.println(obj);
            data[i][0] = obj.getString("ID");
            data[i][1] = obj.getString("value");

            //System.out.println(data[i][0] + " " + data[i][1]);
        }

        // ind the cheapest items by ID
        for (int i = 0; i < data.length; i++){
            String itemID = data[i][0];
            double lowestCostItem = 999999999.99;
            // the distributors and their items are in distributorsData array
            // iterate thro the distributors array and find matching ID with the lowest price
            for (int j = 0; j < dis.size(); j++){
                if(Objects.equals(dis.get(j).get(1), itemID)){
                    lowestCostItem = Math.min(lowestCostItem, Double.parseDouble(dis.get(j).get(2)));
                }
            }
            totalCost = totalCost+(lowestCostItem*Integer.parseInt(data[i][1]));
        }
        System.out.println("Total cost: "+totalCost);
        JSONArray a = new JSONArray();
        JSONObject n = new JSONObject();
        n.put("cost", String.format("%.2f",totalCost));
        a.put(n);
        System.out.println("Sent restock cost");
        return a;
    }

    public static List<List<String>> cleanDistributors(List<List<String>> l){
        List<List<String>> ret = new ArrayList<>();
        // clean garbage values from the list -> I do it by making sure the value at index 1 is a float
        for (List<String> e : l){
            try{
                if (Float.parseFloat(e.get(1)) > 0.0001){
                    ret.add(e);
                }
            }catch(Exception exc){
                continue;
            }

        }

        return ret;
    }

    public static JSONArray arrayToJson(List<List<String>> l){
        // column names
        String[] columns = {"Name", "Stock", "Capacity", "ID"};
        // crete a jsonarray and populate it with the items to restock
        JSONArray jsonAr = new JSONArray();
        for (List<String> e : l){
            JSONObject obj = new JSONObject();
            obj.put(columns[0], e.get(0));
            obj.put(columns[1], e.get(1));
            obj.put(columns[2], e.get(2));
            obj.put(columns[3], e.get(3));
            jsonAr.put(obj);
        }

        return jsonAr;
    }

    public static List<List<String>> findRestockItems(List<List<String>> l){
        // simply remove all rows that have Stock/Capacity < 0.25 and return the 2d arraylist
        l.remove(0); // remove the name of columns at index 0

        List<List<String>> ret = new ArrayList<>();

        //System.out.println((l.get(0).get(1))+(l.get(0).get(1)));
        // calculate the quantity of items that is below 25% and add them to the list to return
        for (List<String> row : l){
            if (Float.parseFloat(row.get(1))/Float.parseFloat(row.get(2)) < 0.25){
                System.out.println("Adding "+row.get(0) + " to the refill list");
                ret.add(row);
            }
        }

        return ret;
    }

    public static JSONArray calculateRestockCost(){

        return null;
    }
}
