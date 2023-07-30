# [Send Emails with Spring Boot](https://www.youtube.com/watch?v=onCzCxDyR24)

Tutorial tomado del canal de [Get Arrays](https://www.youtube.com/watch?v=onCzCxDyR24)

--- 

## Creando Aplicación de Spring Boot

Las dependencias agregadas al proyecto desde su creación en Spring Initializr son la siguientes:

````xml
<!-- Spring Boot 3.1.2 y Java 17-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````

El nombre de la clase principal creada por defecto es ``SpringBootEmailApplication``, lo renombramos a un nombre más
corto ``Main``, lo mismo haremos con la clase principal de Test ``MainTests``.

## User Domain Model

Para el propósito de esta aplicación necesitamos registrar usuarios en la base de datos a quienes luego de registrarse
en nuestra aplicación le tenemos que enviar un email para que confirme su registro. Por lo tanto, necesitamos crear la
clase **User** que será nuestro **Entity** que estará mapeado a una tabla de la base de datos.

````java

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String email;
    private String password;
    private boolean isEnabled;
}
````

## Confirmation Domain Model

Necesitamos una entidad que almacene la confirmación del usuario. Para ser más exactos que almacene el token que se le
enviará, de tal forma que usaremos ese token para validar su confirmación. Entonces, crearemos una entity llamada
**Confirmation**:

````java

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "confirmations")
public class Confirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String token;

    @Temporal(TemporalType.TIMESTAMP)   // (1)
    @CreatedDate                        // (2)
    private LocalDateTime createdDate;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER) // (3)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Confirmation(User user) {
        this.user = user;
        this.createdDate = LocalDateTime.now(); // (2.1)
        this.token = UUID.randomUUID().toString();
    }
}
````

En el código anterior, el tutor usa algunas anotaciones donde vale la pena aclarar algunas de ellas:

### (1) @Temporal(TemporalType.TIMESTAMP)

Según la definición de la anotación **@Temporal():** ``Esta anotación debe especificarse para campos persistentes o
propiedades de tipo java.util.Date y java.util.Calendar. Solo se puede especificar para campos o propiedades de este
tipo.``

Además, para campos del tipo **LocalDateTime en Java, no necesitamos la anotación "@Temporal"**, ya que **LocalDateTime
es una clase de fecha y hora** sin información de zona horaria que **se puede mapear directamente a una columna
TIMESTAMP en la base de datos.**

Por lo tanto, **yo quitaré dicha anotación**, ya que **la propiedad createdDate** al ser del tipo LocalDateTime **se
mapeará automáticamente a la columna "created_date" del tipo TIMESTAMP en la base de datos.**

Recordar que si definimos una propiedad del tipo camelCase en nuestra clase de entidad java, ejemplo: createdDate, su
equivalente como columna en la base de datos sería "created_date" definido por defecto por Hibernate. Podemos usar la
anotación @Column, para cambiar el nombre si quisiéramos.

### (2) @CreatedDate

Esta anotación **se utiliza para marcar un campo de una entidad como la fecha en la que se creó el registro en la base
de datos.** Cuando se guarda por primera vez una nueva instancia de la entidad, **Spring Data Jpa AUTOMÁTICAMENTE
establecerá el valor del campo anotado con @CreatedDate en la fecha y hora actuales.**

Por lo tanto, si observamos el constructor con parámetro vemos (2.1) ``this.createdDate = LocalDateTime.now();``,
**según mi análisis esto ya no debería ir**, ya que la anotación **@CreatedDate** AUTOMÁTICAMENTE lo hace por nosotros.
En consecuencia, yo quitaré dicha línea de código y dejaré que la anotación **@CreatedDate** agregue la fecha
automáticamente por mí.

### Asociación Unidireccional @OneToOne

En el punto **(3)** observamos que la anotación @OneToOne tiene la propiedad **fetch = FetchType.EAGER**. Cuando una
anotación termina en **..One**, por defecto el fetch será del tipo **EAGER**, así que en nuestro caso no lo colocaremos
ya que por defecto lo es.

Otra propiedad que está definida dentro de la anotación @OneToOne es el **targetEntity = User.class**. La definición
dice: ``La clase de entidad que es el destino de la asociación. El valor predeterminado es el tipo de campo o
propiedad que almacena la asociación.``

**targetEntity = User.class:** Este atributo se utiliza para **especificar la clase de destino con la que se establecerá
la relación.** En este caso, la entidad User.class será la otra entidad que se relacionará con la entidad Confirmation
mediante la relación uno a uno.

Por lo tanto, en nuestro caso no es **necesario definir dicho atributo**, ya que JPA/Hibernate utilizará el tipo del
atributo user para inferir la entidad objetivo, que es User en este caso.

### Clase de dominio Confirmation

Finalmente, luego de haber quitado algunas anotaciones y atributos que ya por defecto se establecen, mi clase de dominio
**Confirmation** quedaría de la siguiente manera:

````java

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "confirmations")
public class Confirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String token;

    @CreatedDate
    private LocalDateTime createdDate;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Confirmation(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
    }
}
````

## User Repository

````java
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    Boolean existsByEmail(String email);
}
````

## Confirmation Repository

````java
public interface IConfirmationRepository extends JpaRepository<Confirmation, Long> {
    Optional<Confirmation> findByToken(String token);
}
````

## User Service

Crearemos la interfaz **IUserService**:

````java
public interface IUserService {
    User saveUser(User user);

    Boolean verifyToken(String token);
}
````

Creamos la implementación de la interfaz anterior quien agrupará los dos repositorios que creamos inicialmente.
Antes de registrar al usuario, se hacen ciertas validaciones y se establece el valor de la propiedad **enabled** en
falso.

En el método **verify()** una vez verificado el token, establecemos la propiedad del usuario **enabled** en true, de
esta manera el usuario queda habilitado en el sistema.

````java

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IConfirmationRepository confirmationRepository;

    @Override
    @Transactional
    public User saveUser(User user) {
        if (this.userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(String.format("El email %s ya existe", user.getEmail()));
        }

        user.setEnabled(false);
        this.userRepository.save(user);

        Confirmation confirmation = new Confirmation(user);
        this.confirmationRepository.save(confirmation);

        // TODO enviar email a usuario con token

        return user;
    }

    @Override
    @Transactional
    public Boolean verifyToken(String token) {
        return this.confirmationRepository.findByToken(token)
                .map(confirmationDB -> {

                    String email = confirmationDB.getUser().getEmail();
                    User userDB = this.userRepository.findByEmailIgnoreCase(email)
                            .orElseThrow(() -> new RuntimeException(String.format("No existe el email %s", email)));

                    userDB.setEnabled(true);
                    this.userRepository.save(userDB);
                    this.confirmationRepository.delete(confirmationDB);

                    return Boolean.TRUE;
                })
                .orElseGet(() -> Boolean.FALSE);
    }
}
````
