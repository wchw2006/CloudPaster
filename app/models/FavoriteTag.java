package models;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.modules.ebean.Model;

import models.Paster;
import models.User;
@Entity
@Table(name="favorite_tag")
    public class FavoriteTag extends Model{
	@ManyToOne
	public User user;
	@ManyToOne
	public Tag tag;
    }
