package edu.mcw.scge.directory.process;

import edu.mcw.scge.dao.implementation.GrantDao;
import edu.mcw.scge.dao.implementation.GroupDAO;
import edu.mcw.scge.datamodel.Grant;
import edu.mcw.scge.datamodel.Person;
import edu.mcw.scge.directory.dao.PersonDao;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ProcessFile {
    PersonDao dao=new PersonDao();
    GrantDao grantDao=new GrantDao();
    GroupDAO gdao=new GroupDAO();

    public void insertFromFile(String file) throws Exception {
        dao.updateStatusToInactive("INACTIVE");
        FileInputStream fs=new FileInputStream(new File(file));
        XSSFWorkbook workbook=new XSSFWorkbook(fs);
        XSSFSheet sheet=workbook.getSheet("directory");
        Iterator<Row> rowIterator=sheet.iterator();
        List<Person> persons= new ArrayList<>();
        List<String> initiatives=new ArrayList<>();
        List<String> grants=new ArrayList<>();
        List<String> institutions=new ArrayList<>();
        List<String> names=new ArrayList<>();
        while(rowIterator.hasNext()) {
            Person p = new Person.Builder().build();
            String grantTitle=new String();
            String projectType=new String();
            String institution=new String();

            int grantId= 0;
            int subgroupId= 0;
            int institutionId=0;
            int groupId=0;
            Row row = rowIterator.next();
            String name=new String();
            Iterator<Cell> cellIterator = row.cellIterator();
            List<Integer> roleIds = new ArrayList<>();
            Grant grant=new Grant();
            if (row.getRowNum() != 0) {
                while (cellIterator.hasNext()) {

                    Cell cell = cellIterator.next();
                    int colIndex = cell.getColumnIndex();
                    if (colIndex == 0) {
                        projectType = cell.getStringCellValue();
                        if(!initiatives.contains(projectType.trim().toLowerCase())){
                            initiatives.add(projectType.trim().toLowerCase());
                        }
                        groupId = dao.getGroupId(projectType.trim(), "group");
                    }
                    if (colIndex == 1) {
                        grantTitle = cell.getStringCellValue().trim();
                        grant.setGrantInitiative(projectType.trim());
                        grant.setGrantTitle(grantTitle.trim());
                        grant.setGrantTitleLc(grantTitle.trim().toLowerCase());
                        if(!projectType.equalsIgnoreCase("NIH")) {
                            if (!grants.contains(grantTitle.trim().toLowerCase())) {
                                grants.add(grantTitle.trim().toLowerCase());
                            }
                            grantId = grantDao.insertOrUpdate(grant);
                        }
                        subgroupId = dao.getGroupId(grantTitle, "subgroup");
                    }

                    if (colIndex == 2) {
                        name = cell.getStringCellValue().trim();
                        p.setName(name);
                        p.setName_lc(name.toLowerCase());

                        if (!names.contains(name.toLowerCase())) {
                            names.add(name.toLowerCase());
                        }
                    }
                    if (colIndex == 3) {

                        if (cell.getStringCellValue().trim().equalsIgnoreCase("x")) {
                            int roleId = dao.getRoleId("POC");
                            roleIds.add(roleId);
                        }

                    }
                    p.setStatus("ACTIVE");
                    //    if (colIndex == 4) {
                    if (colIndex == 4) {
                        String role1 = cell.getStringCellValue().trim().toLowerCase();
                        if (role1.contains("administrative")) {
                            role1 = "administrative contact";
                        }
                        int roleId = dao.getRoleId(role1);
                        roleIds.add(roleId);
                    }
                    if (colIndex == 5) {
                        p.setEmail(cell.getStringCellValue().trim());
                        p.setEmail_lc(cell.getStringCellValue().trim().toLowerCase());
                    }
                    if (colIndex == 6) {
                        institution = cell.getStringCellValue().trim();
                        institutionId = dao.insertOrUpdateInstitution(institution);
                        if(!institutions.contains(institution.toLowerCase()))
                            institutions.add(institution.toLowerCase());
                        p.setInstitution(institutionId);
                        p.setInstitutionName(institution);
                    }
                    if (colIndex == 7) {
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_NUMERIC:
                                p.setPhone(String.valueOf(cell.getNumericCellValue()));
                                break;
                            case Cell.CELL_TYPE_STRING:
                                p.setPhone(cell.getStringCellValue().trim());
                        }
                    }
                }
                int personId = dao.insertOrUpdate(p);
                if (personId > 0) {
                    persons.add(p);
                    int defaultGoupId = dao.getGroupId("consortium", "group");
                    gdao.makeAssociations(defaultGoupId, groupId);
                    if (groupId != 0 ) {
                        dao.insertPersonInfo(personId, Arrays.asList(1), groupId, grantId, institutionId);
                    }
                    if (subgroupId != 0)
                        dao.insertPersonInfo(personId, roleIds, subgroupId, grantId, institutionId);
                    gdao.makeAssociations(groupId, subgroupId);

                }
            }


        }
        fs.close();

    }
}
