package com.project.controller;

import com.project.model.Application;
import com.project.model.ApplicationStatus;
import com.project.model.Job;
import com.project.model.Student;
import com.project.repository.ApplicationRepository;
import com.project.repository.JobRepository;
import com.project.repository.StudentRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ApplicationController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    // Display list of job listings
    @GetMapping("/job-listings")
    public String showJobListings(Model model, HttpSession session) {
        Student student = (Student) session.getAttribute("loggedInStudent");

        if (student == null) {
            return "redirect:/login";  // Redirect to login page if student is not logged in
        }

        // Fetch all jobs
        List<Job> jobs = jobRepository.findAll();

        // Pass the application status for each job directly to the view
        for (Job job : jobs) {
            Application application = applicationRepository.findByJobAndStudent(job, student);
            if (application != null) {
                // Set the application status to the enum value
                job.setApplicationStatus(application.getStatus());
            } else {
                job.setApplicationStatus(ApplicationStatus.NOT_APPLIED); // Default enum value for "Not Applied"
            }
        }

        model.addAttribute("jobs", jobs);
        return "job-listings";  // Thymeleaf template for displaying jobs
    }

    // Show the application form for a selected job
    @GetMapping("/apply/{jobId}")
    public String showApplicationForm(@PathVariable Long jobId, Model model, HttpSession session) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Student student = (Student) session.getAttribute("loggedInStudent");

        if (student == null) {
            return "redirect:/login"; // Redirect to login page if student is not logged in
        }

        Application application = new Application();
        application.setJob(job);
        model.addAttribute("applicationForm", application);
        model.addAttribute("student", student); // Pass student details for form submission

        return "application-form"; // Display the application form
    }

    // Handle application form submission
    @PostMapping("/apply")
    public String submitApplication(@ModelAttribute("applicationForm") Application application, Model model, HttpSession session) {
        Student sessionStudent = (Student) session.getAttribute("loggedInStudent");

        if (sessionStudent == null) {
            return "redirect:/login";
        }

        Student student = studentRepository.findById(sessionStudent.getId())
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Job job = jobRepository.findById(application.getJob().getId())
            .orElseThrow(() -> new RuntimeException("Job not found"));

        Application existingApplication = applicationRepository.findByJobAndStudent(job, student);
        if (existingApplication != null) {
            model.addAttribute("message", "You have already applied for this job.");
            model.addAttribute("applicationForm", application);
            return "application-form";
        }

        System.out.println("CGPA being set: " + student.getCgpa());

        application.setStudent(student);
        application.setJob(job);
        application.setCgpa(student.getCgpa());
        application.setStatus(ApplicationStatus.APPLIED);

        applicationRepository.save(application);

        return "redirect:/job-listings";
    }

}
