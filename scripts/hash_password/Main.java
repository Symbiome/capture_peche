import at.favre.lib.crypto.bcrypt.BCrypt;

public class HashRunner {
  public static void main(String[] args) {
    int cost = 12;
    String result = BCrypt.withDefaults().hashToString(cost, args[0].toCharArray());
    System.out.println(result);
  }
}


// javac -cp ~/.m2/repository/at/favre/lib/bcrypt/0.10.2/bcrypt-0.10.2.jar HashRunner.java && java -cp .:~/.m2/repository/at/favre/lib/bcrypt/0.10.2/bcrypt-0.10.2.jar HashRunner 'password'