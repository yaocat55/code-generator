package cn.net.yao.generate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import cn.net.yao.generate.config.GenConfig;

@Controller
public class PageController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("defaultAuthor", GenConfig.getAuthor());
        model.addAttribute("defaultPackage", GenConfig.getPackageName());
        return "index";
    }
}
