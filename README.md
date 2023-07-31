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

Es una anotación que se utiliza en combinación con otras anotaciones **para realizar auditoría.**
Esta anotación **se utiliza para marcar un campo de una entidad como la fecha en la que se creó el registro en la base
de datos.** Cuando se guarda por primera vez una nueva instancia de la entidad, **Spring Data Jpa AUTOMÁTICAMENTE
establecerá el valor del campo anotado con @CreatedDate en la fecha y hora actuales.**

**La anotación @CreatedDate por sí sola no funcionará** de forma automática para establecer la fecha de creación de un
objeto. **Requiere algunas configuraciones adicionales para que funcione correctamente,** como configurar la auditoría
con la anotación **@EnableJpaAuditing**, extender la entidad con clases base de auditoría. **Es imprescindible añadir la
anotación @EnableJpaAuditing para que Spring Boot reconozca las anotaciones de auditoría para nuestra Base de Datos.**

Por lo tanto, como es una anotación orientada a la auditoría, en nuestro caso no será necesario utilizarlo, ya que
para poblar el campo **createdDate** lo haremos manualmente, dentro del constructor que tiene el parámetro del usuario:
**(2.1)** ``this.createdDate = LocalDateTime.now();``.

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

Finalmente, luego de haber quitado algunas anotaciones y atributos que por defecto se establecen o que en nuestro
caso no requerimos, mi clase de dominio **Confirmation** quedaría de la siguiente manera:

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
    private LocalDateTime createdDate;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Confirmation(User user) {
        this.user = user;
        this.createdDate = LocalDateTime.now();
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

## Configuración de la base de datos

Cambiamos el ``application.properties`` por ``application.yml`` y agregamos las siguientes configuraciones:

````yaml
server:
  port: ${SERVER_PORT}

spring:
  profiles:
    active: ${ACTIVE_PROFILE:dev}

  datasource:
    url: jdbc:postgresql://${POSTGRES_SQL_HOST}:${POSTGRES_SQL_PORT}/${POSTGRES_SQL_DB}
    username: ${POSTGRES_SQL_USERNAME}
    password: ${POSTGRES_SQL_PASSWORD}

  jpa:
    generate-ddl: true
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        jdbc:
          time_zone: America/Lima
        globally_quoted_identifiers: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
````

Según se menciona en el tutorial, esta configuración es muy exagerada para el pequeño proyecto que estamos realizando,
pero se hace de esta manera para tener una idea de cómo se realizan las configuraciones en aplicaciones grandes y
desde ya ir adaptándonos.

En las configuraciones tenemos la propiedad **spring.profiles.active** se utiliza para especificar qué perfil (o
perfiles) se debe activar cuando la aplicación se inicie. En nuestro caso utilizamos el siguiente valor para esa
configuración:

````yaml
${ACTIVE_PROFILE:dev}
````

Esta es una notación de expresión que se utiliza para **proporcionar un valor predeterminado en caso de que la variable
de entorno ACTIVE_PROFILE no esté definida.** En este caso, si no se proporciona un valor para ACTIVE_PROFILE, se
utilizará **el perfil dev como valor predeterminado.**

Ahora, también hemos definido otras variables de entorno que no tienen valor predeterminado, tales como:

````yaml
${SERVER_PORT}
${POSTGRES_SQL_USERNAME}
${POSTGRES_SQL_PASSWORD}
${POSTGRES_SQL_HOST}
${POSTGRES_SQL_PORT}
${POSTGRES_SQL_DB}
````

Otra configuración que vale la pena mencionar es la siguiente:

````properties
spring.jpa.properties.hibernate.globally_quoted_identifiers=true
````

Se utiliza para indicar que se deben citar globalmente los identificadores (nombres de tablas y columnas) en las
consultas SQL generadas por Hibernate. Esto significa que todos los nombres de tablas y columnas se incluirán entre
comillas en las consultas SQL generadas, lo que es útil cuando se trabaja con bases de datos que requieren que los
identificadores sean citados.

Con esta configuración, las consultas SQL generadas por Hibernate se verían así:

````roomsql
SELECT `MiEntidad`.`MiColumna` FROM `MiEntidad`;
````

De esta manera, la base de datos entenderá que los nombres de tablas y columnas están citados y procesará las consultas
correctamente.

### Creando perfiles de configuración

Cuando trabajemos en una aplicación grande, por lo general, tendremos distintos entornos donde ejecutaremos nuestra
aplicación: **dev, test, prod** entre otros. Nuestra aplicación debe tener la configuración según el entorno donde será
ejecutado. En nuestro caso tenemos un archivo principal llamado ``application.properties`` donde estamos definiendo
la configuración de la conexión a la base de datos. Esta configuración se establece de manera genérica, utilizando
variables de entorno, de tal forma que, según el ambiente donde sea ejecutado, definiremos los valores para dichas
variables de entorno.

En la raíz del proyecto creamos el perfil para desarrollo: ``application-dev.yml``:

````yaml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_spring_boot_email

#Server
SERVER_PORT: 8081
ACTIVE_PROFILE: dev
````

Ahora creamos el perfil para pruebas: ``application-test.yml``

````yaml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_test

#Server
SERVER_PORT: 8082
ACTIVE_PROFILE: test
````

Ahora creamos el perfil para producción: ``application-prod.yml``

````yaml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_production

#Server
SERVER_PORT: 8083
ACTIVE_PROFILE: prod
````

El archivo por defecto que contiene todas las configuraciones es el ``application.yml``, ahora si queremos definir un
perfil, ya sea **dev, test, prod** o cualquier otro, lo único que haremos será crear con el mismo nombre del archivo
por defecto, agregándole con un guion (-) el perfil que queremos ``application-{nombre_perfil}.properties`` o
``application-{nombre_perfil}.yml``, por ejemplo:

````
# Perfiles

application.yml       (por default)

application-dev.yml   (para desarrollo)
application-test.yml  (para pruebas)
application-prod.yml  (para producción)
````

Ahora, como podemos observar **en cada perfil definimos el valor de las variables de entorno** que se aplicarán a
nuestro archivo por defecto (application.yml). Además, **en cada archivo de configuración específico del perfil, podemos
colocar las propiedades que deseemos personalizar para ese entorno.** Podemos sobrescribir las propiedades del archivo
application.yml o agregar nuevas específicas del perfil.

### Ejecutando perfil usando IntelliJ IDEA

Vamos a ver varias opciones para **ejecutar un perfil determinado usando IntelliJ IDEA**:

````
- Seleccionar Run/Edit Configurations...
- Seleccionamos la configuración de nuestra aplicación llamada: Main
````

Estando en este punto, podemos configurar el perfil de varias maneras. Suponiendo que queremos ejecutar el perfil test:

**Opción 1:** En el input **Environment variables** utilizar la variable de ambiente definida en el **application.yml**

> **Environment variables:** ACTIVE_PROFILE=test

**Opción 2:** En el input **Environment variables** utilizar la configuración de selección de perfil que usa spring
boot:

> **Environment variables:** spring.profiles.active=test

**Opción 3:** En esta opción no usaremos el **Environment variables**, sino que más bien iremos a la opción de
**Modify options** y seleccionaremos **Add VM options**. Nos aparecerá un nuevo input donde escribiremos lo siguiente:

> -Dspring.profiles.active=test

### Ejecutando perfil usando la consola de comandos

Para ejecutar nuestra aplicación seleccionando un perfil, debemos posicionarnos mediante cmd en la raíz del proyecto y
ejecutar el siguiente comando:

````bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
````

Otra opción sería, si generamos el .jar de la aplicación y luego ejecutamos el perfil deseado:

````bash
mvnw clean package
````

````bash
java -jar -Dspring.profiles.active=test .\target\spring-boot-email-0.0.1-SNAPSHOT.jar 
````

````bash
java -jar -DACTIVE_PROFILE=test .\target\spring-boot-email-0.0.1-SNAPSHOT.jar 
````

**IMPORTANTE**

> Si no se especifica ningún perfil activo, Spring Boot utilizará las propiedades definidas en el archivo
> application.yml por defecto. Ahora, este archivo de configuración (que es el por default), en la configuración del
> **spring.profiles.active: ${ACTIVE_PROFILE:dev}** vemos que está usando una variable de entorno llamada
> ACTIVE_PROFILE, y como no especificaremos ningún perfil activo de manera explícita, dicha variable no existirá, por lo
> tanto, tomará el valor del **dev**, de esta manera **se activará el archivo del perfil application-dev.yml.**

## Prueba de humo

Utilizamos curl para hacer la petición a nuestro endpoint y registrar un usuario:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Karen Caldas\", \"email\": \"kasary@gmail.com\", \"password\": \"12345\"}" http://localhost:8081/api/v1/users | jq

--- Response
< HTTP/1.1 201
<
{
  "timeStamp": "2023-07-30T20:28:27.810846300",
  "statusCode": 201,
  "status": "CREATED",
  "message": "Usuario creado",
  "data": {
    "user": {
      "id": 252,
      "name": "Karen Caldas",
      "email": "kasary@gmail.com",
      "password": "12345",
      "enabled": false
    }
  }
}
````

Ahora, revisamos la base de datos para observar los resultados obtenidos:

![registrando-usuario-y-confirmacion.png](./assets/registrando-usuario-y-confirmacion.png)

Y por si quisiéramos ver la asociación generada en la base de datos:

![asociacion-users-confirmations.png](./assets/asociacion-users-confirmations.png)

---

# Email

## Creando contraseña de aplicación Gmail

Utilizaremos nuestro correo de Gmail para poder hacer el envío de correos, pero para no colocar nuestra contraseña real
necesitamos crear una **contraseña de aplicación**.

> Los pasos de la creación de una **Contraseña de Aplicación** para un correo de Gmail se encuentran en el siguiente
> enlace [Iniciar sesión con contraseñas de aplicación](https://support.google.com/accounts/answer/185833?hl=es). De
> todas maneras la colocaré aquí también:
>
> 1. Ve a tu [**cuenta de Google**](https://myaccount.google.com/).
> 2. Selecciona **Seguridad.**
> 3. En "Iniciar sesión en Google", selecciona **Verificación en dos pasos.**
> 4. En la parte inferior de la página, selecciona **Contraseñas de aplicaciones.**
> 5. En el select **seleccionar aplicación** elige **otra (nombre personalizado)** e introduce un nombre que te ayude a
     recordar dónde vas a utilizar la contraseña de aplicación.
> 6. Selecciona **Generar.**
> 7. Se mostrará la contraseña de aplicación generada de 16 caracteres, copiarla y no compartirla con nadie. Esta será
     la contraseña que usemos para enviar los correos desde nuestra aplicación de Spring Boot.
> 8. Selecciona **Hecho.**

## Configuración de Email

En nuestros archivos de configuración definiremos las siguientes variables relacionadas con nuestro servidor de correo
Gmail. Es importante precisar que estas variables de configuración los agregaremos a los distintos ambientes:
**application-dev.yml, application-test.yml, application-prod.yml** y dependiendo del ambiente a trabajar definiremos
sus valores. En nuestro caso, el mismo valor para todos, pero cuando se trabaje en un ambiente real, debemos cambiar
los valores por los que se usen en dicho ambiente:

````yaml
## Other properties

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${SERVER_PORT}
````

Ahora configuraremos el archivo de propiedad principal **application.yml (perfil default):**

````yaml
## Other properties

spring:
  # profiles
  # datasource
  # jpa

  mail:
    host: ${EMAIL_HOST}
    port: ${EMAIL_PORT}
    username: ${EMAIL_ID}
    password: ${EMAIL_PASSWORD}
    default-encoding: UTF-8
    properties:
      mail:
        mime:
          charset: UTF
        smtp:
          writetimeout: 10000       #10s ó 10000 ms
          connectiontimeout: 10000  #10s ó 10000 ms
          timeout: 10000            #10s ó 10000 ms
          auth: true
          starttls:
            enable: true
            required: true
    # Configuración propia personalizada
    verify:
      host: ${VERIFY_EMAIL_HOST}

````

La configuración anterior hace **uso de las variables que definimos en los perfiles de configuración** para configurar
todo lo relacionado con el servidor de mail que usaremos (Gmail).

La siguiente configuración extraída de la configuración anterior, **es personalizada, no es propia de spring.mail**,
pero nosotros podemos agregarlo sin problemas, nuestra configuración personalizada se vería de la siguiente manera
si usáramos la extensión .properties: ``spring.mail.verify.host = ${VERIFY_EMAIL_HOST}``, **es nuestra configuración
propia que posteriormente la usaremos dentro de la aplicación.**

````yml
spring:
  mail:
    # Configuración propia personalizada
    verify:
      host: ${VERIFY_EMAIL_HOST}
````

**NOTA**

> La configuración del servidor de correo también la podemos hacer usando una clase de java y utilizando las variables
> definidas en los archivos de configuración yml, pero según el tutor, es mejor utilizar el archivo de configuración.

