package controllers;

import java.util.List;
import java.util.Map;

import models.Paster;
import models.Event;
import models.Action;
import models.Paster.QueryResult;
import models.Paster.Type;
import models.Subscribe;
import models.Tag;
import models.User;
import net.sf.oval.constraint.NotEmpty;
import notifiers.Notifier;

import org.apache.commons.lang.StringUtils;

import play.mvc.Controller;
import play.mvc.With;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;

@With({Deadbolt.class,GlobalUser.class})
public class CloudPaster extends Controller {
	final static int PAGE_SIZE=30;
    /**
     * 最近活跃
     */
    static public void activity() {
        List<Paster> pasters = Paster.find("type=? order by created desc,updated desc ", Type.Q).fetch(PAGE_SIZE);
        render(pasters);
    }

    /**
     * 问题
     */
    static public void questions(int from) {
        long count = Paster.count("type=?", Type.Q);
        List<Paster> pasters = Paster.find("type = ? order by created desc", Type.Q).from(from).fetch(PAGE_SIZE);
	long pagesize = PAGE_SIZE;
	List<Map> clouds = Tag.find(
				    "select new map(t.name as name, count(p.id) as count) from Paster p join p.tags as t group by t.name order by count(p.id) desc"
				    ).fetch();
        render(pasters, from, count, pagesize, clouds);
    }

    /**
     * 等待回答
     */
    static public void unanswered(int from) {
        long count = Paster.count("type=? and answerCount = 0", Type.Q);
        List<Paster> pasters = Paster.find("type=? and answerCount = 0 order by created desc", Type.Q).from(from).fetch(PAGE_SIZE);
		long pagesize = PAGE_SIZE;
        render(pasters, from, count, pagesize);
    }

    /**
     * 标签和分类
     */
    static public void tags() {
        List<Map> clouds = Tag.find(
            "select new map(t.name as name, count(p.id) as count) from Paster p join p.tags as t group by t.name order by count(p.id) desc"
        ).fetch();
        render(clouds);
    }

    static public void tag(String name, int from) {
        long count = Paster.count("select distinct count( p) from Paster p join p.tags as t where t.name = ?", name);
        List<Paster> pasters = Paster.find("select distinct p from Paster p join p.tags as t where t.name = ?", name).from(from).fetch(PAGE_SIZE);
		long pagesize = PAGE_SIZE;
        render(pasters, from, count, pagesize, name);
    }

    /**
     * 用户
     */
    static public void users() {
        render();
    }

    static public void hide(long id) {
        Paster paster = Paster.findById(id);
        paster.hide();
        if (paster.type == Type.A || paster.type == Type.C) {
            while (paster.parent != null) {
                paster = paster.parent;
            }
        }
        view(paster.id);
    }

    @Restrictions(@Restrict("user"))
    static public void comment(long id, long aid, String content) {
        Paster paster = Paster.findById(id);
        if (StringUtils.isNotEmpty(params.get("docommentadd"))) {
            Paster.comment(aid > 0 ? aid : id, content, Auth.getLoginUser());
            view(id);
        }
        if (StringUtils.isNotBlank(params.get("docancel"))) {
            view(id);
        }
        String state = "comment";
        if (aid > 0) {
            state = "answer-comment";
        }
        render("@view", paster, state, aid);
    }

    @Restrictions(@Restrict("user"))
    static public void answer(long id, long aid, String content) {
        if (StringUtils.isNotEmpty(params.get("doansweradd"))) {
            Paster answer = Paster.answer(id, content, Auth.getLoginUser());
            if(Subscribe.count("user = ? and topic = ?", Auth.getLoginUser(),Subscribe.TOPIC_ANSWER_FOR_ME)>0)
            	Notifier.anwser(answer.parent, answer);
            view(id);
        }
        if (StringUtils.isNotEmpty(params.get("doupdateanswer"))) {
            Paster answer = Paster.findById(aid);
            if (answer.creator.id == Auth.getLoginUser().id) {
                Paster.update(aid, answer.title, content, Auth.getLoginUser(), null);
            }
            view(id);
        }
        if (StringUtils.isNotBlank(params.get("docancel"))) {
            view(id);
        }
        Paster paster = Paster.findById(id);
        String state = "answer";
        render("@view", paster, state);
    }

    /**
     * 提问
     */
    @Restrictions(@Restrict("user"))
    static public void ask(Long id, @NotEmpty String title,
            @NotEmpty String content, String tagstext) {
        String state = "";
        if (StringUtils.isNotEmpty(params.get("doupdatequestion")) && id > 0) {
            Paster tmp = Paster.findById(id);
            if (Auth.getLoginUser().id == tmp.creator.id) {
                Paster paster = Paster.update(id, title, content, Auth.getLoginUser(), tagstext);
                view(paster.id);
            } else {
                flash.error("你不能修改别人的问题");
            }
        }
        if (StringUtils.isNotEmpty(params.get("doprequery"))) {
            params.flash();
            QueryResult search = Paster.search(title, 0, 5);
            List<Paster> recommendPosts = search.results;
            if (search.count > 0) {
                render(recommendPosts);
            } else {
                state = "newask";
                render(state);
            }
        }
        if (StringUtils.isNotEmpty(params.get("newask"))) {
            params.flash();
            state = "newask";
            render(state);
        }
        if (StringUtils.isNotEmpty(params.get("doaddask"))) {
            params.flash();
            Paster paster = Paster.create(title, content, Auth.getLoginUser(), tagstext);
            List<Map> subscribeUsers = User.find("select distinct new map(u.name as name,u.email as email) from User as u , Subscribe s where s.user = u").fetch();
            Notifier.newquestion(subscribeUsers, paster);
            view(paster.id);
        }
        state = "start";
        render(state);
    }
    @Restrictions(@Restrict("user"))
    public static void edit(long id) {
        Paster paster = Paster.findById(id);
        if(paster.creator.id != Auth.getLoginUser().id){
	    jsonresult("failed","permission-limited", "你不能修改别人的问题",0);
	    return;
        }
        String state = "answer-edit";
        if (paster.type == Type.A) {
            paster = paster.parent;
            render("@view", paster, state, id);
        }
        state = "question-edit";
        render("@view", paster, state);
    }

    public static void view(long id) {
        Paster paster = Paster.findById(id);
        paster.viewCount ++;
        paster.save();
        render(paster);
    }

    public static void search(String keywords, int from) {
        if (StringUtils.isNotEmpty(keywords)) {
            QueryResult search = Paster.search(keywords, from, PAGE_SIZE);
            long count = search.count;
            List<Paster> pasters = search.results;
			long pagesize = PAGE_SIZE;
            render(pasters, count, from, pagesize, keywords);
        } else {
            flash.error("请输入要查询的内容。");
            render();
        }
    }

    static void jsonresult(String state, String code, String msg,int newscore){
        request.format = "json";
        response.contentType = "application/json";
        render("@jsonresult", state, code, msg, newscore);
    }

    public static void voteup(long id) {
        User user = Auth.getLoginUser();
	if(user == null){
	    jsonresult("failed","need-login","请登录",0);
	}
        Paster paster = Paster.findById(id);
        Event event = Event.find("action in(?,?) and user=? and target=?", Action.Votedown,Action.Voteup, user, paster).first();
        if (event != null) {
	    event.delete();
	    if(event.action == Action.Votedown){
		paster.votedown(false);
	    }else{
		paster.voteup(false);
		jsonresult("ok","vote-success","已经取消",paster.voteup-paster.votedown);
	    }
        }
	Event newevent = new Event();
	newevent.action = Action.Voteup;
	newevent.user = user;
	newevent.target = paster;
	newevent.save();
	paster.voteup(true);
	jsonresult("ok","vote-success","顶上了",paster.voteup-paster.votedown);
    }

    public static void votedown(long id) {
        User user = Auth.getLoginUser();
	if(user == null){
	    jsonresult("failed","need-login","请登录",0);
	}
        Paster paster = Paster.findById(id);
        Event event = Event.find("action in(?,?) and user=? and target=?", Action.Votedown,Action.Voteup, user, paster).first();
        if (event != null) {
	    event.delete();
	    if(event.action == Action.Voteup){
		paster.voteup(false);
	    }else{
		paster.votedown(false);
		jsonresult("ok","vote-success","已经取消",paster.voteup-paster.votedown);
	    }
        }
	Event newevent = new Event();
	newevent.action = Action.Votedown;
	newevent.user = user;
	newevent.target = paster;
	newevent.save();
	paster.votedown(true);
	jsonresult("ok","vote-success","踩中了",paster.voteup-paster.votedown);
    }
}
