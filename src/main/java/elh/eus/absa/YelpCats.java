/*
 * Copyright 2014 Elhuyar Fundazioa

This file is part of EliXa.

    EliXa is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    EliXa is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with EliXa.  If not, see <http://www.gnu.org/licenses/>.
 */

package elh.eus.absa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.*;

public class YelpCats {	
	
	private Set<String> categories = new HashSet<String>();
	public YelpCats(File in) throws IOException
	{
		BufferedReader breader = new BufferedReader(new FileReader(in));
		// one category per line
		String line;
		while ((line = breader.readLine()) != null) 
		{
			String cat = line.trim();
			categories.add(cat);
		}
	}
	
	
	/*public static void main(String[] args) throws IOException 
	{
		File file1 = new File(args[0]);
		if (file1.exists())
		{
			YelpCats mainclass = new YelpCats((file1));
			mainclass.ExtractCats(System.in, System.out);
		}
	}*/

	public void ExtractCats(InputStream in, PrintStream out) throws JSONException, IOException
	{
		BufferedReader breader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		String jsonline;
		System.err.println(this.categories.size()+" categories accepted\n");
		while ((jsonline = breader.readLine()) != null) 
		{						
			JSONObject jsonObject = new JSONObject(jsonline);
			// get a String from the JSON object
			String b_id = (String) jsonObject.get("business_id");
			// get categories of the object
			JSONArray cats = jsonObject.getJSONArray("categories");
			for(int i=0; i<cats.length(); i++)
			{		
				String cat = cats.get(i).toString();
				//System.out.println(cats.get(i));
				if (this.categories.contains(cat))
				{
					bwriter.write(b_id+"\n");
					continue;	
				}
			}		
		}
		bwriter.close();
		breader.close();
	}
	
	public void ExtractReviews(InputStream in, PrintStream out) throws JSONException, IOException
	{
		BufferedReader breader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		Set<String> printedRevs = new HashSet<String>();
		String jsonline;
		System.err.println(this.categories.size()+" categories accepted\n");
		while ((jsonline = breader.readLine()) != null) 
		{						
			JSONObject jsonObject = new JSONObject(jsonline);
			// get a String from the JSON object
			String b_id = jsonObject.getString("business_id");
			// get review id of the object
			String rev_id = jsonObject.getString("review_id");
			// get review score of the object
			Long rev_score = jsonObject.getLong("stars");
			if (this.categories.contains(b_id) && (! printedRevs.contains(rev_id)))
			{	
				// get review text of the object
				String text = jsonObject.getString("text");
				text = text.replaceAll("\n", " ");
				bwriter.write(rev_id+"\t"+b_id+"\t"+rev_score+"\t"+text+"\n");
				printedRevs.add(rev_id);	
			}
		}
		bwriter.close();
		breader.close();
	}
}
