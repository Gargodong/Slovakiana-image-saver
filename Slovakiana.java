import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.OutputStream;
import java.io.FileOutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class Slovakiana
{
    private static String comma = ",";
    
    private static String rightBracket = "]";
    
    private static String slash = "/";
    
    private static String jp2 = ".jp2";
    
    private static String digitalObjectDoImage = "sk.cair.mdwh.client.entity.digitalobject.DoImage";

    private static String[] output = new String[7];
    
    private static String object;
    
    private static FileOutputStream fosLog;
    
    private static boolean generateBoth = true;
    
    private static int placeid = -1;
    
    public static void main(String[] args) 
    {
        System.out.println("Starting...");

	int length = args.length;

	int character = -1;

	String parameter = null;

	switch (length)
	{
		case 1:
		{
			character = args[0].charAt(0);

			if (character > 47 && character < 58)
			{
				parameter = args[0];
			}
			else
			{
				generateBoth = false;
			}
		}
		break;

		case 2:
		{
			character = args[0].charAt(0);

			if (character > 47 && character < 58)
			{
				parameter = args[0];
			}
			else
			{
				generateBoth = false;
			}

			character = args[1].charAt(0);

			if (character > 47 && character < 58)
			{
				parameter = args[1];
			}
			else
			{
				generateBoth = false;
			}
		}
		break;

		default:
		case 0:
		break;
	}
        
        if (parameter != null)
        {
            try 
            {
                placeid = Integer.parseInt(parameter);
            }
            catch (NumberFormatException exception) 
            {
                System.out.println("Error parsing parameter.");
            
                placeid = -1;
            }
        }
        
        int position = -1;
        
        int count = -1;
        int pages = -1;
        
        try
        {
            fosLog = new FileOutputStream("Output.csv", false);
        }
        catch (IOException exception)
	    {
	       System.out.println("Error creating log.");
	    }
	    
	    String slovakiana = readStringFromFile("Slovakiana.txt");
        
        String requestString = slovakiana;
        
        String responseString = sendStringRequest(requestString);
        
        count = findCount(responseString);
        pages = findPages(responseString);
        
        System.out.println("Founded " + count + " objects in " + pages + " page(s).");
        
        // Strip page number from string.
            
        slovakiana = slovakiana.substring(0, slovakiana.length() - 1);
        
        int pagesFull = pages - 1;
        
        int remainder = count - (pagesFull * 100);
        
        for (int i = 1; i < pagesFull + 1; i++)
        {
            System.out.println("Process page " + i);
            
            // Add new page number to string.
        
            requestString = slovakiana + String.valueOf(i);
            System.out.println(requestString);
            
            // Send request.
            
            responseString = sendStringRequest(requestString);
            
            // Process full (100) pages.
            
            processPage(responseString, 100, i - 1);
        }
        
        System.out.println("Process page " + pages);
            
        // Add new page number to string.
        
        requestString = slovakiana + String.valueOf(pages);
        System.out.println(requestString);
            
        // Send request.
            
        responseString = sendStringRequest(requestString);
        
        // Process partial page.
        
        processPage(responseString, remainder, pages - 1);
        
        try
        {
            fosLog.close();
        }
        catch (IOException exception)
	    {
	       System.out.println("Error closing log.");
	    }
    }
    
    private static void processPage(String responseString, int count, int pages)
    {
        for (int i = 0; i < count; i++)
        {
            int position = findCair(responseString);
            
            String cair = responseString.substring(position, position + 12); // cair-XXXXXXX
            
            int objectid = i + 1;
            
            if (pages < 10)
            {
                object = "0";
            }
            else
            {
                object = "";
            }
            
            if (objectid < 10)
            {
                object = object + String.valueOf(pages) + "0" + String.valueOf(objectid);
            }
            else
            {
                if (objectid == 100)
                {
                    if (pages == 9)
                    {
                        object = String.valueOf(pages + 1) + "00";
                    }
                    else
                    {
                        object = object + String.valueOf(pages + 1) + "00";
                    }
                    
                }
                else
                {
                    object = object + String.valueOf(pages) + String.valueOf(objectid);
                }
            }
            
            System.out.println("Object " + object + ": " + cair);

            String doid = sendSlovakianaRequest1(cair);
            
            sendSlovakianaRequest2(doid);
            
            responseString = responseString.substring(position + 12);
        }
    }
    
    private static void sendSlovakianaRequest2(String identifier)
    {
        String slovakianaDigitalObject = "https://wcm.slovakiana.sk/digitalobject/";
            
        // Send request and retrieve response.

        String requestString = slovakianaDigitalObject + identifier;
        
        String responseString = sendStringRequest(requestString);
        
        // Content.
        
        int position = responseString.indexOf("content");

        String content = responseString.substring(position);
        
        // Download.
        
        getDownload(content);
    }
    
    private static String sendSlovakianaRequest1(String identifier)
    {
        String slovakianaCulturalObject = "https://wcm.slovakiana.sk/culturalobject/";
        
        // Send request and retrieve response.
        
        String requestString = slovakianaCulturalObject + identifier;
        
        String responseString = sendStringRequest(requestString);
        
        // Attributes.
        
        int position = responseString.indexOf("attributes");
        
        String attributes = responseString.substring(position);
        
        parseAttributes(attributes);
        
        // Digital objects.
        
        position = responseString.indexOf("digitalObjects");

        String digitalObjects = responseString.substring(position);
        
        position = digitalObjects.indexOf("id");

        String doid = digitalObjects.substring(position);

        doid = doid.substring(5,15);
        
        return doid;
    }
    
    private static int findCair(String text)
    {
        return text.indexOf("cair");
    }
    
    private static int findCount(String text)
    {
        int count = -1;
        
        int positionStart = text.indexOf("count");
        String temp = text.substring(positionStart + 7);
        
        int positionStop = temp.indexOf(comma);
        
        String temp2 = temp.substring(0, positionStop);
 
        try 
        {
            count = Integer.parseInt(temp2);
        }
        catch (NumberFormatException exception) 
        {
            System.out.println("Error parsing count.");
            
            count = -1;
        }
        
        return count;
    }
    
    private static int findPages(String text)
    {
        int pages = -1;
        
        int positionStart = text.indexOf("pages");
        String temp = text.substring(positionStart + 7);
        
        int positionStop = temp.indexOf(comma);
        
        String temp2 = temp.substring(0, positionStop);
        
        try 
        {
            pages = Integer.parseInt(temp2);
        }
        catch (NumberFormatException exception) 
        {
            System.out.println("Error parsing pages.");
            
            pages = -1;
        }
        
        return pages;
    }
    
    private static String readStringFromFile(String fileName) 
    {
        String output = null;
        
		try 
		{
			output = new String(Files.readAllBytes(Paths.get(fileName)));
		}
		catch (IOException exception) 
		{
		    System.out.println("Error reading input file.");
		    
			return null;
		}
		
		return output;
	}
    
    private static String sendStringRequest(String requestString)
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestString)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
            return response.body();
        }
        catch (IOException | InterruptedException exception)
        {
            System.out.println("Error sending request.");
            
            return null;
        }
       
    }
    
    private static byte[] sendByteRequest(String requestString)
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestString)).build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (200 == response.statusCode()) 
            {
                return response.body();
            }
            else
            {
                System.out.println("Response returns " + response.statusCode() + ".");
                return null;
            }
        }
        catch (IOException | InterruptedException exception)
        {
            System.out.println("Error sending request.");
            
            return null;
        }
    
    }
    
    private static void getDownload(String content)
    {
        int position = -1;
        
        int numOfImages = 1;
        
        if (generateBoth)
        {
            numOfImages = 2;
        }
        
        for (int i = 0; i < numOfImages; i++)
        {
            position = content.indexOf(digitalObjectDoImage);
            
            int positionStop = position + digitalObjectDoImage.length();
            
            String dodi = content.substring(position);

	        position = dodi.indexOf("full");

	        String full = dodi.substring(position);

	        position = full.indexOf("fileUrl");

	        String fileUrl = full.substring(position);

	        position = fileUrl.indexOf(jp2);

	        String download = fileUrl.substring(10, position + 4);
	        
	        content = content.substring(positionStop);
	        
	        // Send request and retrieve response.
	        
	        byte[] bytes = sendByteRequest(download);
	        
	        String streamName;
	        
	        if (generateBoth)
	        {
	            int ix = i + 1;
	        
	            streamName = object + "_" + "p" + ix + jp2;
	        }
	        else
	        {
	            streamName = object + jp2;
	        }
	       
	        try
	        {
	            FileOutputStream fos = new FileOutputStream(streamName);
                    fos.write(bytes);
                    fos.close();
	        }
	        catch (IOException exception)
	        {
	            System.out.println("Error writing image.");
	        }
        }
    }
    
    private static void parseStreet()
    {
        String hyphen = "-";
        
        String street = output[2];
        
        int position = street.indexOf(comma);
        String streetName = street.substring(0, position);
        
        if (streetName.equals(hyphen))
        {
            streetName = "";
        }
        
        String streetNumber = street.substring(position + 1);
        
        position = streetNumber.indexOf(slash);
        String streetNumber1 = streetNumber.substring(1, position); // Add 1 to skip space
        
        if (streetNumber1.equals(hyphen))
        {
            streetNumber1 = "";
        }
        
        String streetNumber2 = streetNumber.substring(position + 1);
        
        if (streetNumber2.equals(hyphen))
        {
            streetNumber2 = "";
        }
        
        output[2] = streetName;
        output[5] = streetNumber1;
        output[6] = streetNumber2;
    }
    
    private static void parseAttributes(String attributes)
    {
        int positionStart = -1;
      
        int positionStop = -1;

        String values = "values";
        
        for (int i = 0; i < 5; i++)
        {
            // Values.
        
            positionStart = attributes.indexOf(values);
            positionStart += 6; // values
            positionStart += 4; // ":["
        
            positionStop = attributes.indexOf(rightBracket);
            positionStop -= 1; // "
        
            output[i] = attributes.substring(positionStart, positionStop);
        
            positionStop += 6; // "]},{"
            attributes = attributes.substring(positionStop);
        }
        
        parseStreet();
        
        // Log.
            
        createLog();
    }
    
    private static void createLog()
    {
        String newline = "\r\n";
        
        if (placeid != -1)
        {
            object = "110" + placeid + object;
        }
        
        try
        {
            fosLog.write(object.getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[0].getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[1].getBytes());
            fosLog.write(comma.getBytes());
	    fosLog.write(output[6].getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[2].getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[5].getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[3].getBytes());
            fosLog.write(comma.getBytes());
            fosLog.write(output[4].getBytes());
            fosLog.write(newline.getBytes());
        
        }
        catch (IOException exception)
        {
            System.out.println("Error writing log.");
        }
    }
}
