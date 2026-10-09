package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "seed.play-review-student.enabled", havingValue = "true")
public class PlayReviewStudentSeederRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlayReviewStudentSeederRunner.class);

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final GenderRepository genderRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;

    public PlayReviewStudentSeederRunner(
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository,
            GenderRepository genderRepository,
            YearRepository yearRepository,
            SemesterRepository semesterRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.genderRepository = genderRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("PLAY REVIEW STUDENT SEEDER: Checking and seeding pragatix.play.review@gmail.com...");

            Department cyberDept = getOrCreateCyberDepartment();
            Section sectionA = getOrCreateSectionA(cyberDept);
            Year year1 = getOrCreateYear1();
            Semester sem1 = getOrCreateSemester1();
            Gender gender = getOrCreateGender();

            String email = "pragatix.play.review@gmail.com";
            String fullName = "Student";
            String mobile = "1597538462";
            String regNo = "811324104999";
            String sprNo = "SPR8113REVIEW";

            Student student = studentRepository.findAll().stream()
                    .filter(s -> (s.getEmail() != null && s.getEmail().trim().equalsIgnoreCase(email))
                            || (s.getRegNo() != null && s.getRegNo().trim().equalsIgnoreCase(regNo)))
                    .findFirst()
                    .orElse(null);

            if (student == null) {
                student = new Student();
                student.setEmail(email);
                student.setRegNo(regNo);
                student.setSprNo(sprNo);
            }

            student.setEmail(email);
            student.setPassword("");
            student.setFullName(fullName);
            student.setPhoneNo(mobile);
            student.setDepartment(cyberDept);
            student.setSection(sectionA);
            student.setYearRef(year1);
            student.setSemesterRef(sem1);
            student.setYear(year1.getYearName() != null ? year1.getYearName() : "1st Year");
            student.setSemester(sem1.getSemesterName() != null ? sem1.getSemesterName() : "Semester 1");
            student.setGenderRef(gender);
            student.setGender(gender.getGenderName() != null ? gender.getGenderName() : "Male");
            student.setActive(true);
            student.setDeleted(false);

            if (student.getTotalXp() == 0) {
                student.setScore(0);
                student.setTotalXp(0);
                student.setGroupXp(0);
                student.setIndividualXp(0);
                student.setMustXp(0);
                student.setStage(1);
                student.setCurrentStage(1);
            }

            studentRepository.save(student);
            log.info("PLAY REVIEW STUDENT SEEDER: Student pragatix.play.review@gmail.com successfully seeded/updated! (Dept: {}, Sec: {}, Year: {}, Sem: {})",
                    cyberDept.getName(), sectionA.getSectionName(), year1.getYearName(), sem1.getSemesterName());

        } catch (Exception e) {
            log.error("PLAY REVIEW STUDENT SEEDER: Error seeding review student: {}", e.getMessage(), e);
        }
    }

    private Department getOrCreateCyberDepartment() {
        List<Department> list = departmentRepository.findAll();
        for (Department d : list) {
            String name = d.getName() != null ? d.getName().toLowerCase() : "";
            String deptName = d.getDeptName() != null ? d.getDeptName().toLowerCase() : "";
            String code = d.getDeptCode() != null ? d.getDeptCode().toLowerCase() : "";
            if (name.contains("cyber") || deptName.contains("cyber") || code.contains("cyber") || code.contains("cy")) {
                return d;
            }
        }
        Department d = new Department();
        d.setDeptCode("CYBER");
        d.setDeptName("Computer Science and Engineering (Cyber Security)");
        d.setName("Computer Science and Engineering (Cyber Security)");
        return departmentRepository.save(d);
    }

    private Section getOrCreateSectionA(Department dept) {
        if (dept != null && dept.getId() != null) {
            List<Section> list = sectionRepository.findByDepartment_Id(dept.getId());
            for (Section s : list) {
                if ("A".equalsIgnoreCase(s.getSectionName()) || "Section A".equalsIgnoreCase(s.getSectionName())) {
                    return s;
                }
            }
        }
        Section s = new Section();
        s.setSectionName("A");
        s.setDepartment(dept);
        return sectionRepository.save(s);
    }

    private Year getOrCreateYear1() {
        List<Year> list = yearRepository.findAll();
        for (Year y : list) {
            if (y.getYearNo() == 1 || (y.getYearName() != null && (y.getYearName().contains("1") || y.getYearName().toLowerCase().contains("first")))) {
                return y;
            }
        }
        Year y = new Year();
        y.setYearNo((byte) 1);
        y.setYearName("1st Year");
        return yearRepository.save(y);
    }

    private Semester getOrCreateSemester1() {
        List<Semester> list = semesterRepository.findAll();
        for (Semester sem : list) {
            if (sem.getSemesterNo() == 1 || (sem.getSemesterName() != null && (sem.getSemesterName().contains("1") || sem.getSemesterName().toLowerCase().contains("first")))) {
                return sem;
            }
        }
        Semester sem = new Semester();
        sem.setSemesterNo((byte) 1);
        sem.setSemesterName("Semester 1");
        return semesterRepository.save(sem);
    }

    private Gender getOrCreateGender() {
        List<Gender> list = genderRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Gender g = new Gender();
        g.setGenderName("Male");
        return genderRepository.save(g);
    }
}
