/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import java.util.List;

import com.avaje.ebean.Query;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;
import models.ConfigItem;
import play.Play;
import play.libs.Mail;
import play.modules.ebean.EbeanSupport;
import play.mvc.Controller;
import play.mvc.With;

/**
 *
 * @author nile
 */
@With(Deadbolt.class)
public class Manager extends Controller{
	@Restrictions(@Restrict("admin"))
    public static void sysinfo(){
        final Runtime runtime = Runtime.getRuntime();  
        Query<ConfigItem> query = ConfigItem.all();
		final List<ConfigItem> configs = query.findList();
        render(runtime,configs);
    }
        @Restrictions(@Restrict("admin"))
    public static void delconfig(long id) {
    	ConfigItem item = ConfigItem.findById(id);
    	item.delete();
    	sysinfo();
    }
        @Restrictions(@Restrict("admin"))
    public static void addconfig(ConfigItem item) {
    	ConfigItem.createOrSave(item);
    	sysinfo();
    }
        @Restrictions(@Restrict("admin"))
    public static void apply() {
    	Query<ConfigItem> query = ConfigItem.all();
		final List<ConfigItem> configs = query.findList();
    	for (ConfigItem item : configs) {
    		Play.configuration.put(item.name, item.val);
		}
    	Mail.session = null;
    	flash.success("配置成功应用。");
    	sysinfo();
    }
}
