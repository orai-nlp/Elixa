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

public class Opinion {

	private String id; //opinion id to link the opinion with the corresponding sentence. Format is "o[0-9]+"
	private String target; //opinion target string
	private Integer from; //initial offset of the opinion target string
	private Integer to; //final offset of the opinion target string
	private String polarity; //polarity of the opinion 
	private String category; //opinion target category value=global if polarity correspond to the whole sentence and not to a target
	private String sId;   //sentence id to link the opinion with the corresponding sentence. Format is "s[0-9]+"
	
	public Opinion (String id, String trgt, Integer f, Integer t, String p, String c, String s)
	{
		setId(id);
		setTarget(trgt);
		setFrom(f);
		setTo(t);
		setPolarity(p);
		setCategory(c);
		setsId(s);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/**
	 * @return the from
	 */
	public Integer getFrom() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(Integer from) {
		this.from = from;
	}

	/**
	 * @return the to
	 */
	public Integer getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(Integer to) {
		this.to = to;
	}

	/**
	 * @return the polarity
	 */
	public String getPolarity() {
		return polarity;
	}

	/**
	 * @param polarity the polarity to set
	 */
	public void setPolarity(String polarity) {
		this.polarity = polarity;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the sId
	 */
	public String getsId() {
		return sId;
	}

	/**
	 * @param s the sId to set
	 */
	public void setsId(String s) {
		this.sId = s;
	}

	
	
}
