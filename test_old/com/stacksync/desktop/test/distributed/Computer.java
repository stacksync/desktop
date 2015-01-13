package com.stacksync.desktop.test.distributed;

public class Computer {

	private String ip = "";
	private String username= "";
	private String password= "";
    
    public Computer(String ip, String username, String password){
        this.ip = ip;
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    @Override
    public String toString(){
        return "ip:"+ip+" username:"+username+" password:"+password;
    }
}