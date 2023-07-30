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

## User Resource

Antes de implementar el recurso de usuario, vamos a crear una clase que sea común a todas las respuestas que se manden
desde el backend:

````java

@JsonInclude(JsonInclude.Include.NON_DEFAULT)   // (1)
@SuperBuilder                                   // (2)
@Data
public class HttpResponse {
    protected String timeStamp;
    protected int statusCode;
    protected HttpStatus status;
    protected String message;
    protected String developerMessage;
    protected String path;
    protected String requestMethod;
    protected Map<?, ?> data;
}
````

De esta clase es importante aclarar algunas anotaciones:

### (1) @JsonInclude(JsonInclude.Include.NON_DEFAULT)

Se puede usar para excluir las propiedades con valores predeterminados de POJO.

**Si se usa @JsonInclude(JsonInclude.Include.NON_DEFAULT) en el nivel de clase, se excluyen los valores predeterminados
de los campos.** Esto se hace creando una instancia de POJO utilizando un constructor de cero argumentos y comparando
los valores de propiedad, excluyendo los que tienen valores predeterminados, **por ejemplo, el valor int predeterminado
es 0, el valor de String predeterminado es nulo y así sucesivamente.**

Por ejemplo:

````java

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Employee {
    private String name;
    private String dept;
    private Integer salary;
    private boolean fullTime;
    private List<String> phones;
    private Date dateOfBirth;
    /* other code */
}
````

````java
public class ExampleMain {
    public static void main(String[] args) throws IOException {
        Employee employee = new Employee();
        employee.setName("Trish");
        employee.setFullTime(false);
        employee.setPhones(new ArrayList<>());
        employee.setSalary(Integer.valueOf(0));
        employee.setDateOfBirth(new Date(0));

        ObjectMapper om = new ObjectMapper();
        String jsonString = om.writeValueAsString(employee);
        System.out.println(jsonString);
    }
}
````

El resultado sería:

````json
{
  "name": "Trish",
  "salary": 0,
  "phones": [],
  "dateOfBirth": 0
}
````

Como se ha visto en la salida anterior, **solo se excluyeron las propiedades con valores de miembro predeterminados.**
No se excluye el Integer con 0 (primitive wrapper - salary), ya que su valor predeterminado sería null y no 0, no se
excluye la fecha con 0 milisegundos y tampoco se excluye la colección "vacía" (teléfonos). Pero sí se excluye el
departamento, porque al no ser definido su valor, por defecto es nulo, también se excluye la propiedad
fullTime, porque al ser un primitivo booleano, su valor predeterminado es **false** y cuando creamos el objeto le
estamos asignando **false**, su valor predeterminado.

### (2) @SuperBuilder

Nos permite la creación de objetos mediante el patrón de diseño **Builder**. Esta anotación es similar a la anotación
**@Builder**, también de Lombok, pero con una diferencia.

Cuando se coloca la anotación **@Builder** en una clase, Lombok genera un constructor privado con todos los campos de la
clase y un método estático público llamado builder() que devuelve una instancia del Builder. Este Builder se utiliza
para configurar selectivamente los campos de la clase y, finalmente, crear una instancia de la clase llamando al método
build(). **Es útil cuando solo necesitas crear una clase con el patrón de diseño Builder, sin la necesidad de heredar de
una clase base con sus propios campos.**

Ahora, **@SuperBuilder es una extensión de @Builder.** Además de generar el Builder para la clase anotada, también
**tiene en cuenta la herencia.** Cuando una clase utiliza @SuperBuilder, el Builder generado tiene métodos para
configurar tanto los campos de la clase actual como los campos heredados de la clase base. Esto **permite construir
objetos de la clase derivada junto con sus campos heredados.**

Es útil cuando tienes una jerarquía de clases y deseas construir objetos de clases derivadas utilizando el patrón de
diseño Builder.

Cuando todas las clases están anotadas con **@SuperBuilder**, obtenemos un constructor para la clase secundaria que
también expone las propiedades de los padres.

**Tenga en cuenta que tenemos que anotar todas las clases.** @SuperBuilder no se pueden mezclar con @Builder dentro de
la misma jerarquía de clases. Si lo hace, se producirá un error de compilación.

Ahora sí, creamos nuestro recurso de usuario donde implementamos dos end points:

````java

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/users")
public class UserResource {

    private final IUserService userService;

    @PostMapping
    public ResponseEntity<HttpResponse> createUser(@RequestBody User user) {
        User userDB = this.userService.saveUser(user);
        URI uriUser = URI.create("");
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("user", userDB))
                .message("Usuario creado")
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
        return ResponseEntity.created(uriUser).body(httpResponse);
    }

    @GetMapping
    public ResponseEntity<HttpResponse> confirmUserAccount(@RequestParam String token) {
        Boolean isSuccess = this.userService.verifyToken(token);
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("success", isSuccess))
                .message("Cuenta verificada")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
        return ResponseEntity.ok(httpResponse);
    }
}
````
