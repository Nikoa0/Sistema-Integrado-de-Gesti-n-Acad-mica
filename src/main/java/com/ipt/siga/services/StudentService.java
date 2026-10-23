package com.ipt.siga.services;

import com.ipt.siga.entities.Student;
import com.ipt.siga.repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    public Student saveStudent(Student student) {
        // Validación de negocio básica para el estado del estudiante
        if (student.getStatus() == null || student.getStatus().isEmpty()) {
            student.setStatus("REGULAR"); // Por defecto entra como alumno regular
        }
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
}