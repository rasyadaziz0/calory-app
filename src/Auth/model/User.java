package Auth.model;

public class User {
    private String id;
    private String email;
    private String name ;
    private String accessToken;

    //Constructor Kosong buat Json nya
    public User(){}

    public User (String id, String email, String name, String accessToken) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.accessToken = accessToken;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName(){
        return name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "User{" + "id='" + id + '\'' + ", email='" + email + '\'' + ", name='" + name + '\'' + ", accessToken='" + accessToken + '\'' +'}';
    }
}