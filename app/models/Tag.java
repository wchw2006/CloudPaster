package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;
@Entity
public class Tag extends Model {
	public String name;
	public Date lastPaste;
	public Tag(String name) {
		this.name= name;
	}
	public static Tag findOrCreateByName(String name) {
	    Tag tag = Tag.find("byName", name).first();
	    if(tag == null) {
	        tag = new Tag(name);
	        tag.save();
	    }
	    return tag;
	}
}