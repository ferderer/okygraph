package de.ferderer.site;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.View;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.class,
})
@Controller
public class App {

    public static void main(String... args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/")
    public View index() {
        return new HomeView();
    }

    @GetMapping("/about")
    public View about() {
        return new AboutView();
    }

    @GetMapping("/projects")
    public View projects() {
        return new ProjectsView();
    }

    @GetMapping("/blog")
    public View blog() {
        return new BlogView();
    }

    @GetMapping("/blog/{slug}")
    public View blogEntry() {
        return new BlogEntryView();
    }
}
