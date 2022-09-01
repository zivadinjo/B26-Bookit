package com.bookit.pages;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.Map;
public class SelfPage extends BasePage{
    @FindBy(xpath = "//p[.='name']/preceding-sibling::p")
    public WebElement fullName;

    @FindBy(xpath = "//p[.='role']/preceding-sibling::p")
    public WebElement role;

//    public Map<String, String> getUserInfo() {
//
//    }
}

