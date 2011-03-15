package models;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.commons.lang.StringUtils;

import play.db.jpa.Model;
import play.modules.search.Field;
import play.modules.search.Indexed;
import play.modules.search.Query;
import play.modules.search.Search;
import util.PasterUtil;
import util.TokenUtil;
@Entity
@Indexed
public class Paster extends Model {
	@Field
	public String content;
	@Field(tokenize=true,sortable=true)
	public String title;
	@OneToOne
	public Paster parent ;
	@OneToOne
	public Paster best;
	@ManyToOne
	public User creator;
	public Date created;
	public Type type = Type.Q;
	public String tagstext;
	@ManyToMany
	public Set<Tag> tags = new HashSet<Tag>();
	@OneToOne
	public User lastUser;
	public Date updated;
	public State state = State.NORMAL;
	public int voteup;
	public int votedown;
	public int answerCount;
	public int commentCount;
	
	public static enum Type{
		Q,A,C
	}
	public static enum State{
		NORMAL,HIDDEN
	}
	public static Paster comment(long parentId,String content,User user) {
		return createAndSave(null,content, user,null, -1,parentId,Type.C);
	}
	public static Paster answer(long parentId,String content,User user) {
		return createAndSave(null,content, user,null, -1,parentId,Type.A);
	}
	public static Paster create(String title,String content,User user,String tags) {
		return createAndSave(title,content, user,tags, -1,-1,Type.Q);
	}
	public static Paster update(long id ,String title,String content,User user,String tags) {
		return createAndSave(title,content, user,tags, id,-1,null);
	}
	public static Paster createAndSave(String title,String content,User user,String tagstext,long id,long parentId,Type type) {
		Paster paster = null;
		if(id > 0) {
			paster = Paster.findById(id);
			paster.lastUser = user;
			paster.updated = new Date();
		} else {
			paster = new Paster();
			paster.created= new Date();
			paster.type = type;
			paster.creator = user;
		}
		content = PasterUtil.cleanUpAndConvertImages(content, user.email);
		paster.content = content;
		paster.title = title;
		paster.tagstext = tagstext;
		if(parentId>0) {
			Paster tmp_parent = paster.findById(parentId);
			if(type== Type.C)
				tmp_parent.commentCount++;
			if(type == Type.A)
				tmp_parent.answerCount++;
			paster.parent = tmp_parent;
			paster.parent.save();
		}
		if(paster.tagstext!=null) {
			String[] tagNames = tagstext.trim().split("[ ,;]");
			for (String tag : tagNames) {
				if(StringUtils.isNotEmpty(tag)) {
					System.out.println("========"+tag+"!!");
					paster.tagWith(tag);
				}
			}
		}
		paster.save();
		return paster;
	}
	public Paster tagWith(String tag) {
		this.tags.add(Tag.findOrCreateByName(tag));
		return this;
	}
	public static long countByCreator(String email){
		return Paster.count("byCreator", email);
	}
	public static List<Paster> findByCreator(String email,int from,int pagesize){
		JPAQuery find = Paster.find("creator=? order by createDate desc",email);
		if(from>0)
			find.from(from);
		return find.fetch(pagesize);
	}
	public static List<Paster> findAll(int from ,int pagesize){
		return findAll(from, pagesize,"order by createDate desc");
	}
	public static List<Paster> findMostUseful(int from ,int pagesize){
		return findAll(from, pagesize,"order by useful desc");
	}
	public static List<Paster> findAll(int from ,int pagesize,String order){
		return Paster.find(order).from(from).fetch(pagesize);
	}
	public static List<Paster> findAll(int from ,int pagesize,String query,String order){
		return Paster.find(query +" " + order).from(from).fetch(pagesize);
	}
	
	public void remove() {
		delete();
	}
	public void voteup() {
		voteup+=1;
		save();
	}
	public void votedown() {
		votedown+=1;
		save();
	}
	public static class QueryResult{
		public long count;
		public List<Paster> results;
		public QueryResult(List<Paster> list,long count) {
			this.results = list;
			this.count = count;
		}
	}
	public static QueryResult search(String keywords,int from,int pagesize) {
		String query ="content:" + StringUtils.join(TokenUtil.token(keywords)," AND content:");
		Query q = Search.search(query, Paster.class);
		q.orderBy("title")
		    .page(from*pagesize,pagesize)
		    .reverse();
		List<Paster> fetch = q.fetch();
		return new QueryResult(fetch,q.count());
	}
	public List<Paster> getAnswers() {
		return Paster.find("state = ? and parent.id = ? and type=?", State.NORMAL,this.id,Type.A).fetch();
	}
	public List<Paster> getComments() {
		return Paster.find("state = ? and parent.id= ? and type=?", State.NORMAL, this.id,Type.C).fetch();
	}
	public void hide() {
		this.state = State.HIDDEN;
		if(parent!=null && this.type == Type.A) {
			parent.answerCount--;
			parent.save();
		}
		if(parent!=null && this.type == Type.C) {
			parent.commentCount--;
			parent.save();
		}
		save();
	}
}
