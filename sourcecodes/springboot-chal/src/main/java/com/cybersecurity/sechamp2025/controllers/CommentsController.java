package com.cybersecurity.sechamp2025.controllers;

import com.cybersecurity.sechamp2025.models.Book;
import com.cybersecurity.sechamp2025.services.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class CommentsController {

    @Autowired
    private BookService bookService;

    @GetMapping("/comments")
    public String commentsPage(Model model) {
        // Load all books from the database
        List<Book> books = bookService.findAll();
        List<String> categories = bookService.getCategories();
        
        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Book Comments");
        
        return "comments";
    }
}