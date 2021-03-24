package edu.mcw.scge.directory.dao;

import edu.mcw.scge.dao.AbstractDAO;
import edu.mcw.scge.dao.implementation.GroupDAO;
import edu.mcw.scge.dao.spring.IntListQuery;
import edu.mcw.scge.dao.spring.PersonQuery;
import edu.mcw.scge.dao.spring.StringListQuery;
import edu.mcw.scge.datamodel.Person;
import edu.mcw.scge.datamodel.SCGEGroup;


import java.util.ArrayList;
import java.util.List;

public class PersonDao extends AbstractDAO {

    GroupDAO gdao=new GroupDAO();
    public void insert(Person p) throws Exception{
        String sql="insert into person(person_id,name, name_lc,  email, email_lc," +
                "phone, address, google_id,status, created_date, modified_date, modified_by, other_id, " +
                "first_name," +
                "last_name)" +
                " values(?,?,?,?,?,?,?,?,?,current_date,current_date,?,?,?,?)";
        update(sql,
                p.getId(), p.getName(),p.getName().toLowerCase(),
                p.getEmail(),p.getEmail().toLowerCase(),
                p.getPhone(), p.getAddress(), p.getGoogleSub(),
                p.getStatus(),
                p.getModifiedBy(),
                p.getOtherId(),
                p.getFirstName(),p.getLastName()
        );

    }
    public void update(Person p) throws Exception {
        String sql="update person set status=?, modified_date=current_date where person_id=?";
        update(sql, p.getStatus(),p.getId());
    }

    public List<Person> getPersonByName(String name) throws Exception{
        String sql="select * from person where name_lc=? " ;
        PersonQuery query=new PersonQuery(this.getDataSource(), sql);
        return execute(query,name);
    }

    public List<Person> getAllMembers() throws Exception{
        String sql="select * from person";
        PersonQuery query=new PersonQuery(this.getDataSource(), sql);
        return query.execute();
    }

    public void updateStatusToInactive(String status) throws Exception {
        String sql="update person set status=?";
        update(sql, status);
    }



    public void insertInstitution(int id,String name) throws Exception {

        String sql="insert into institution values(?,?,?)";
        try {
            update(sql, id, name, name.toLowerCase());
        }catch (Exception e){
            System.err.println("Institution: "+ name);
            e.printStackTrace();
        }

    }
    public int insertOrUpdateInstitution(String name) throws Exception {
        String sql="select institution_id from institution where institution_name=?";
        IntListQuery query=new IntListQuery(this.getDataSource(), sql);
        List<Integer> ids= execute(query, name);
        int id= 0;
        if(ids==null || ids.size()==0){
            id= getNextKey("institution_seq");
            insertInstitution(id, name);
        }else{
            id=ids.get(0);
        }
        return id;
    }

    public int getGroupId(String groupName, String groupType) throws Exception {
        String sql = "select group_id from scge_group where group_name_lc=? and group_type=?";
        IntListQuery query = new IntListQuery(this.getDataSource(), sql);
        List<Integer> ids = execute(query, groupName.toLowerCase().trim(), groupType);
        int id = 0;
        if (ids != null && ids.size() > 0) {
            gdao.updateGroupName(ids.get(0), groupName.trim());
            return ids.get(0);
        }else{
            id=getNextKey("group_seq");
            SCGEGroup group=new SCGEGroup();
            group.setGroupId(id);
            group.setGroupName(groupName.trim());
            group.setGroupType(groupType);
            group.setGroupNameLC(groupName.toLowerCase().trim());
            gdao.insert(group);
        }
        return id;

    }
    public int getRoleId(String role) throws Exception {
        int roleKey=0;
        String sql="select role_key from scge_roles where role=?";
        IntListQuery query=new IntListQuery(this.getDataSource(), sql);
        List<Integer> roles=execute(query, role);
        if(roles!=null && roles.size()>0){
            return roles.get(0);
        }else{
            roleKey=getNextKey("role_seq");
            insertRole(role, roleKey);
        }
        return roleKey;
    }
    public void insertRole(String role, int roleKey) throws Exception {
        String sql="insert into scge_roles (role_key, role) values (?,?)";
        update(sql, roleKey, role);
    }
    public String getRole(String role) throws Exception {
        String sql="select role from scge_roles where role=?";
        StringListQuery query=new StringListQuery(this.getDataSource(), sql);
        List<String> roles=execute(query, role);
        if(roles!=null && roles.size()>0){
            return roles.get(0);
        }else{
            return "member";
        }
    }


    public void insertPersonInfo(int personId, List<Integer> roleIds,int groupId, int grantId, int institutionId ) throws Exception {
        for(int role:roleIds){
            if(!isPersonInfoExists(personId, role, groupId, institutionId)){
                insertPersonInfo(personId, role, groupId,grantId, institutionId);
            }
        }
    }
    public boolean isPersonInfoExists(int personId, int role, int groupId, int institutionId) throws Exception {
        List<Integer> personInfo= getPersonInfo(personId, role, groupId, institutionId);
        if(personInfo!=null && personInfo.size()>0){
            return true;
        }else
            return false;
    }

    public List<Integer> getPersonInfo(int personId, int role, int groupId, int institutionId) throws Exception {
        String sql="select person_id from person_info where person_id=? and role_key=? and group_id=? and institution_id=?";
        IntListQuery query=new IntListQuery(this.getDataSource(), sql);
        return execute(query, personId, role, groupId, institutionId);
    }

    public void insertPersonInfo(int personId, int roleId,int groupId, int grantId, int institutionId ) throws Exception {

        String sql="insert into person_info(person_id, " +
                "group_id," +
                "role_key," +
                "grant_id, institution_id) values(?,?,?,?,?)";
        update(sql, personId,  groupId, roleId, grantId, institutionId);


    }
    public  List<Person> getPersonRecords(Person p) throws Exception {
        List<Person> members=new ArrayList<>();
        String name=p.getName().replaceAll("[,.]", "");
        for(Person person:   getAllMembers()){
            try {
                String str1 = person.getName().replaceAll("[,.]", "");
                if (name.equalsIgnoreCase(str1) ||
                        p.getEmail().toLowerCase().equalsIgnoreCase(person.getEmail().toLowerCase())) {
                    members.add(person);
                }

            }catch (Exception e){e.printStackTrace();}
        }
        return members;
    }
    public int insertOrUpdate(Person p) throws Exception {
        int id=0;
        List<Person> members=new ArrayList<>();
        members=getPersonByName(p.getName().toLowerCase().trim());
        if(members==null || members.size()==0){
            members=getPersonRecords(p);

            if (members==null || members.size()==0) {

                id= getNextKey("person_seq");
                p.setId(id);
                try{
                    insert(p);
                }catch (Exception e){
                    e.printStackTrace();
                    return 0;
                }
            }
        }
        if(members!=null && members.size()>0){
            boolean active=false;
            for(Person person: members){
                if(person.getStatus().equalsIgnoreCase("ACTIVE")){
                    active=true;
                    id=person.getId();
                    break;
                }
            }
            if(!active) {
                p.setId(members.get(0).getId());

            }else{
                p.setId(id);

            }
            update(p);
        }

        return id;
    }

}

