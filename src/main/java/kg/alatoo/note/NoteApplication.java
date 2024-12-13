package kg.alatoo.note;

import kg.alatoo.note.config.Jwttokens;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NoteApplication {

    private Jwttokens jwttokens;
    public void setJwttokens(Jwttokens jwttokens){
        this.jwttokens=jwttokens;
    }

    public static void main(String[] args) {
        SpringApplication.run(NoteApplication.class, args);
    }

}
