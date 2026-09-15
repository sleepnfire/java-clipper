# Pourquoi une entité n'a pas de setters

> **Leçon.** Rendre impossible à la compilation ce qui ne doit jamais arriver à l'exécution :
> le *record* verrouille le DTO **par le type**, les méthodes métier verrouillent l'entité
> **par l'intention**.

Ce document explique le *pourquoi*. Le refactor correspondant est déjà appliqué dans `clipper` :
tous les exemples « avant » sont du vrai code qui a tourné dans ce dépôt, tous les exemples
« après » sont le code actuel.

---

## 1. L'intention, concrètement

La règle n'est pas « les setters sont interdits ». Énoncée comme une interdiction, elle devient un
dogme qu'on applique de travers. La formulation exacte est :

> Une entité ne doit exposer **aucune mutation qui ne corresponde pas à un événement métier nommé**.

Tout tient dans le mot **intention**, et ce mot reste flou tant qu'on n'en donne pas une définition
opérationnelle. C'est l'objet de cette section.

### 1.1 Définition

> Une méthode exprime une **intention** quand son nom et ses paramètres décrivent **ce qui arrive
> dans le métier**, et non **quel champ prend quelle valeur**.

Un setter décrit une écriture mémoire. Une intention décrit un **événement** : une production, une
expédition, une livraison, une correction d'inventaire. La différence n'est pas cosmétique, parce qu'un événement
porte trois choses qu'une écriture ne porte pas : une **cause**, une **règle de validité**, et des
**conséquences solidaires**.

### 1.2 Ce qu'un setter efface

Voici trois situations réelles du projet. Regarde ce qui arrive dans le code :

| Ce qui se passe dans le métier | Ce que le code écrit |
|---|---|
| L'usine expédie 40 trombones vers un magasin | `factory.setStock(60)` |
| Un inventaire corrige une erreur de comptage | `factory.setStock(60)` |
| Un bug de calcul produit une valeur fausse | `factory.setStock(60)` |

**Trois causes différentes, un seul appel identique.** Le setter est le point où l'information se
perd : le stock vaut 60, et plus personne ne sait pourquoi. Or c'est précisément le « pourquoi »
qui porte la règle — une expédition doit vérifier le stock disponible, un inventaire non.

Une fois le « pourquoi » effacé, la règle n'a plus d'endroit où vivre dans l'entité. Elle remonte
donc chez l'appelant, et elle y remonte **autant de fois qu'il y a d'appelants**.

C'est le mécanisme complet de l'anti-pattern, en une phrase : *le setter n'externalise pas la
donnée, il externalise la raison.*

### 1.3 Le test du paramètre — le critère décisif

C'est le test le plus rapide, et il tranche presque tous les cas :

> **Un setter reçoit le résultat. Une intention reçoit l'événement.**

```java
factory.setStock(60);   // 60 = l'état d'APRÈS
factory.ship(40);       // 40 = ce qui SE PASSE
```

Regarde qui fait l'arithmétique :

- Avec `setStock(60)`, **l'appelant a déjà calculé** `100 - 40`. Donc il a déjà, à ce moment-là,
  décidé si l'opération était permise. L'entité ne reçoit qu'un verdict, elle ne peut plus
  l'examiner. Elle est réduite à obéir.
- Avec `ship(40)`, **l'entité calcule**. Elle est donc en position de refuser, parce qu'elle voit
  à la fois l'état actuel et l'ampleur de l'événement.

**C'est là que se joue tout le reste.** Un appelant qui fait l'arithmétique est un appelant qui
applique une règle métier — et il n'a aucun moyen de savoir s'il l'applique correctement. Le bug
de `StoreServiceImp` (§2) n'est pas autre chose que ça : `factory.setStock(factory.getStock() - quantity)`.

### 1.4 Le test de l'énoncé

Deuxième test, complémentaire : **lis l'appel à voix haute, en français, à quelqu'un qui connaît
le métier mais pas Java.**

| Code | À voix haute | Verdict |
|---|---|---|
| `factory.ship(40)` | « L'usine expédie 40 trombones. » | ✅ une phrase du métier |
| `store.receive(30)` | « Le magasin reçoit 30 trombones. » | ✅ |
| `shipment.markDelivered(now)` | « L'expédition est livrée maintenant. » | ✅ |
| `factory.setStock(60)` | « L'usine met son stock à 60. » | ❌ personne ne parle comme ça |
| `shipment.setStatus(DELIVERED)` | « L'expédition met son statut à livrée. » | ❌ |

Si la phrase ne peut pas être dite à un client, à un logisticien ou au rédacteur du cahier des
charges, ce n'est pas une intention : c'est une écriture mémoire déguisée.

### 1.5 Les faux amis

Renommer un setter ne crée pas une intention. Trois pièges classiques, dont **deux viennent de ce
dépôt** :

| Signature | Passe le test du paramètre ? | Passe le test de l'énoncé ? | Verdict |
|---|---|---|---|
| `setStock(int stock)` | ❌ reçoit le résultat | ❌ | mutation |
| `updateStock(int stock)` | ❌ reçoit le résultat | ❌ | mutation **renommée** |
| `addStock(int quantity)` | ✅ reçoit l'événement | ❌ nomme le champ, pas la cause | à mi-chemin |
| `receive(int quantity)` | ✅ | ✅ « le magasin reçoit 12 trombones » | intention |

`addStock` est le cas intéressant, et c'est le nom qui existait réellement dans `Store`. Il passe
le premier test : il reçoit bien une quantité, pas un total. Mais il échoue au second, et ça se
paie : *pourquoi* le stock augmente-t-il ? Une livraison qui arrive ? Un retour client ? Un
inventaire ? Chaque cas a des règles distinctes, et `addStock` les confond à nouveau.

**Le signal d'alarme :** si tu ne peux nommer la méthode qu'en citant le champ qu'elle modifie
(`Stock`, `Status`, `Date`), tu n'as pas encore trouvé l'intention — tu as trouvé un synonyme de
setter.

### 1.6 Une intention est indivisible

Troisième propriété, celle qui rend des états impossibles. Une intention ne porte pas seulement un
nom : elle porte **tout ce que l'événement implique, en un seul geste**.

« L'expédition est livrée » implique trois choses, simultanément et sans exception :

```java
public void markDelivered(LocalDateTime deliveredAt) {
    if (this.status == ShipmentStatus.DELIVERED) {
        throw new IllegalStateException("L'expédition #" + this.id + " est déjà livrée");
    }
    this.status = ShipmentStatus.DELIVERED;
    this.deliveredAt = deliveredAt;
    this.store.receive(this.quantity);
}
```

Écrites en trois setters, ces conséquences deviennent **optionnelles** : on peut en faire une et
oublier les deux autres. Écrites en une intention, elles sont solidaires par construction. On ne
peut plus « livrer à moitié », parce que la phrase n'existe pas.

### 1.7 Ce que « verrouiller par l'intention » veut dire exactement

La citation du haut oppose deux verrous, et l'opposition est réelle :

|  | Verrou du **type** (record) | Verrou de l'**intention** (entité) |
|---|---|---|
| Qui contrôle | le **compilateur** | le **vocabulaire** |
| Ce qui est empêché | écrire `request.quantity = -5` → **ne compile pas** | écrire « retirer 1000 du stock » → **ne s'exprime pas** |
| Mécanisme | les champs sont `final` | les seules méthodes publiques sont les gestes légaux |

Un record t'empêche d'écrire quelque chose d'interdit. Une entité bien conçue fait plus subtil :
elle ne te donne **pas les mots** pour le dire. `factory.ship(1000)` s'écrit et compile très bien —
mais il échoue à l'exécution, systématiquement, parce que la règle est dans le seul chemin
possible. Il n'existe aucune porte dérobée par laquelle un appelant pressé pourrait passer.

L'image juste est celle d'une API d'échecs : on n'expose pas `setPiece(case, pièce)` avec une note
« pensez à vérifier que le coup est légal ». On expose `move(pièce, case)`, et la légalité est
vérifiée dedans. Le plateau ne peut jamais entrer dans une position impossible — non pas parce
qu'on l'a interdit, mais parce qu'aucune suite d'appels ne permet d'y arriver.

### 1.8 Récapitulatif : les trois événements de `clipper`

Le stock d'une usine ou d'un magasin varie pour **trois raisons distinctes**, et chacune a sa
propre règle :

| Intention | Effet sur `stock` | Règle propre | Méthode |
|---|---|---|---|
| L'usine produit | `+= production` | aucune limite haute | `Factory.produce()` |
| L'usine expédie | `-= quantité` | **refus si stock insuffisant** | `Factory.ship(q)` |
| Le magasin reçoit | `+= quantité` | quantité strictement positive | `Store.receive(q)` |

Trois intentions, trois règles. Un seul `setStock` les écrasait toutes en un geste neutre — et
c'est pour ça que les règles s'étaient dispersées dans les services.

---

## 2. Trois symptômes, observés dans ce dépôt

### Symptôme 1 — Un invariant sans gardien

L'invariant le plus élémentaire du projet est `stock >= 0`. Avant le refactor, voici où il vivait
(`StoreServiceImp`, version d'origine) :

```java
if (factory.getStock() < quantity) {
    throw new ConflictException("Stock insuffisant : ...");
}

// … 8 lignes de calcul de distance et de durée …

factory.setStock(factory.getStock() - quantity);
```

Trois problèmes, par ordre de gravité croissante :

1. **La garde et la mutation sont séparées par 8 lignes.** Rien ne les lie. Une refonte du calcul
   de distance peut insérer un `return` anticipé entre les deux sans que rien ne proteste.
2. **La règle est dans le service, pas dans l'usine.** Elle protège *ce site d'appel*, pas
   l'entité. Un autre service, un job batch, un test, un futur `ShipmentService` : chacun devra
   se souvenir de réécrire le `if`.
3. **Chaque nouveau besoin duplique la règle.** Le jour où un second geste retire du stock —
   une mise au rebut, un transfert entre usines, un retour fournisseur — son auteur devra
   se souvenir d'écrire le même `if`. Deux copies d'un invariant, c'est deux occasions de
   diverger, et zéro garantie que la seconde soit écrite.

Aujourd'hui, la règle est là où sont les données :

```java
// Factory.java
public void ship(int quantity) {
    requirePositive(quantity, "La quantité expédiée doit être positive");
    if (this.stock < quantity) {
        throw new ConflictException("Stock insuffisant : l'usine '" + this.name
                + "' a " + this.stock + " trombones, " + quantity + " demandés");
    }
    this.stock -= quantity;
}
```

et le service ne fait plus que déclarer l'intention :

```java
// StoreServiceImp.supplyStore — le if a disparu, il n'a pas été supprimé : il a déménagé
factory.ship(quantity);
```

**Ce qui a changé conceptuellement :** avant, `stock >= 0` était une *convention* que le code
appelant devait connaître. Maintenant c'est une *propriété de l'objet*. La différence n'est pas
stylistique — c'est la différence entre « on espère » et « on garantit ».

### Symptôme 2 — Un état impossible restait représentable

`DeliveryService`, version d'origine :

```java
shipment.setStatus(ShipmentStatus.DELIVERED);
shipment.setDeliveredAt(LocalDateTime.now());
shipment.getStore().addStock(shipment.getQuantity());
```

Ces trois lignes forment **une seule transition métier** : « l'expédition arrive ». Écrites comme
trois mutations indépendantes, elles rendent exprimables des états qui n'existent pas dans la
réalité :

- `DELIVERED` avec `deliveredAt == null` → `NullPointerException` garantie dans le premier calcul
  de délai de livraison ;
- `DELIVERED` sans crédit du stock magasin → des trombones qui disparaissent de la comptabilité ;
- une expédition livrée **deux fois** → stock crédité en double.

Pire, avec `@Builder` on pouvait écrire :

```java
Shipment.builder().status(DELIVERED).departedAt(demain).build();  // compilait parfaitement
```

Une expédition **livrée avant d'être partie**.

La correction ne consiste pas à « ajouter des vérifications ». Elle consiste à retirer les moyens
de créer ces états :

```java
// Le constructeur ne prend PAS status en paramètre : une expédition naît en transit, point.
public Shipment(Factory factory, Store store, int quantity, double distanceKm,
                LocalDateTime departedAt, LocalDateTime estimatedArrivalAt) {
    …
    this.status = ShipmentStatus.IN_TRANSIT;
}

// Une seule transition, atomique par construction.
public void markDelivered(LocalDateTime deliveredAt) {
    if (this.status == ShipmentStatus.DELIVERED) {
        throw new IllegalStateException("L'expédition #" + this.id + " est déjà livrée");
    }
    this.status = ShipmentStatus.DELIVERED;
    this.deliveredAt = deliveredAt;
    this.store.receive(this.quantity);   // la livraison EST l'entrée en stock
}
```

`DeliveryService` passe de 3 lignes à 1 :

```java
shipment.markDelivered(LocalDateTime.now());
```

**Le point de la leçon :** « expédition livrée sans date » n'est plus un bug qu'on corrige, c'est
une phrase qu'on ne peut plus écrire. C'est ça, « impossible à la compilation ».

### Symptôme 3 — L'identité et le verrou étaient publics

`Factory` et `Store` portaient `@Setter` au niveau classe. Donc, gratuitement :

```java
factory.setId(42L);
factory.setVersion(7L);
Factory.builder().id(42L).version(7L).build();   // un objet qui prétend être une ligne existante
```

`id` et `version` n'appartiennent pas au code applicatif : ils appartiennent au provider JPA. Et
`version` n'est pas un champ décoratif — c'est le jeton d'*optimistic locking* sur lequel repose
toute la boucle de 3 retries de `supplyStore` :

```java
for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try { … } catch (OptimisticLockException e) { … }
}
```

Une machinerie de concurrence soigneusement construite, adossée à un jeton que n'importe quel
appelant pouvait réécrire. C'est le cas d'école du setter généré « pour ne pas écrire de
boilerplate » qui ouvre une brèche que personne n'a jamais décidé d'ouvrir.

**La leçon générale :** `@Setter` au niveau **classe** n'est pas une décision de conception, c'est
une absence de décision. Il génère un mutateur pour chaque champ, y compris ceux dont on n'a
jamais voulu qu'ils soient mutables. Les annotations de confort ont un coût : elles rendent le
défaut permissif invisible.

---

## 3. Pourquoi ça arrive toujours (la pente naturelle)

Personne n'a *décidé* que `stock` serait librement mutable. La séquence est mécanique :

1. On crée une entité JPA. On croit savoir qu'il « faut » des getters et des setters.
2. Lombok propose `@Getter @Setter` en une ligne. On prend.
3. Le premier besoin métier arrive : retirer du stock. Le setter existe déjà, donc on l'utilise
   depuis le service — c'est le chemin le plus court.
4. La règle de validation doit bien aller quelque part : elle va juste au-dessus, dans le service.
5. Le deuxième besoin arrive. On copie-colle le `if`.
6. Au bout de six mois, l'entité est un sac de données et toute la logique est dans les services.

C'est précisément ce que Martin Fowler décrit en 2003 sous le nom d'**Anemic Domain Model** :

> *« little more than bags of getters and setters »* … *« there are a set of service objects which
> capture all the domain logic, carrying out all the computation and updating the model objects »*

Et son verdict :

> *« The fundamental horror of this anti-pattern is that it's so contrary to the basic idea of
> object-oriented design; which is to combine data and process together. »*

> *« They incur all of the costs of a domain model, without yielding any of the benefits. »*

Le coût : on a payé le mapping objet-relationnel, la gestion du cycle de vie des entités, les
transactions. Le bénéfice qu'on devait acheter avec — un modèle qui protège ses règles — on ne l'a
pas eu. On a écrit de la programmation procédurale avec des annotations JPA autour.

En termes DDD (Evans, 2003, ch. 6) : `Factory` et `Store` sont des **agrégats**, donc des
frontières de cohérence. Un agrégat est responsable de faire respecter ses propres invariants.
Quand la vérification vit chez l'appelant, l'invariant n'est plus garanti — il est seulement
*espéré, à chaque site d'appel*.

---

## 4. L'objection technique : « mais JPA a besoin des setters »

C'est le blocage n°1, et il est **factuellement faux** — avec une condition déjà remplie dans ce
projet.

Dans `clipper`, les annotations `@Id`, `@Column`, `@Version` sont posées **sur les champs**. Or le
placement de `@Id` détermine la stratégie d'accès. Champ → Hibernate lit et écrit les champs
**directement par réflexion**. Les getters et setters sont purement et simplement **ignorés**.

Ce que la spécification Jakarta Persistence exige réellement d'une entité :

- un constructeur **sans argument**, `public` **ou `protected`** — donc `protected` suffit ;
- la classe et les champs persistants non `final`.

Elle n'exige **nulle part** de setters. Le *dirty checking* d'Hibernate compare un snapshot des
champs au moment du flush ; il ne compte pas les appels de setters.

D'où la ligne qui remplace `@NoArgsConstructor` dans les trois entités :

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // requis par JPA, interdit au code métier
```

`protected` satisfait exactement la spec, et dit au reste du code : *ce constructeur n'est pas
pour toi*. Vlad Mihalcea formule le même constat côté mutateurs : la signature `void setX(T)`
n'est qu'une convention JavaBean, et *« if your application doesn't use the JavaBean setters,
you're better off changing the setter signature »*.

**Autrement dit : il n'y a aucun compromis technique à faire.** Le setter public sur une entité
JPA n'achète strictement rien à la persistance. Il ne coûte que de la sûreté.

---

## 5. Le symétrique : pourquoi le record fait le même travail côté DTO

C'est la seconde moitié de la leçon, et la symétrie n'est pas une coïncidence.

Un DTO ne transporte rien de plus qu'une photo : les valeurs reçues sur le fil, à un instant
donné. Sa bonne propriété n'est donc pas « avoir des règles métier », c'est **ne plus jamais
changer** une fois validé.

Un record fait exactement ça. [JEP 395](https://openjdk.org/jeps/395) définit les records comme
des *« transparent carriers for immutable data »* : les champs dérivés des composants sont `final`.

La conséquence pratique est décisive pour `@Valid` :

- Sur un **record**, valider une fois au constructeur canonique **suffit**. L'instance est garantie
  satisfaire ses invariants pour toujours — il n'existe aucun chemin de code qui puisse la
  dégrader ensuite.
- Sur un **bean mutable**, `@Valid` ne prouve rien au-delà de l'instant de la validation. Un
  `setQuantity(-5)` quelque part plus loin dans la chaîne ne sera jamais re-contrôlé. La
  validation devient un rituel plutôt qu'une garantie.

Donc, deux objets, deux natures, **deux verrous différents pour un seul principe** :

|  | DTO | Entité |
|---|---|---|
| Ce que c'est | une photo de valeurs | un objet vivant avec des règles |
| Ce qu'il doit interdire | **le changement** | **le changement incohérent** |
| Le verrou | le **type** : `record`, champs `final` | l'**intention** : `ship()`, `receive()`, `markDelivered()` |
| Validé | une fois, au constructeur | à chaque transition, par la méthode |

Dans `clipper`, les DTO (`StoreRequest`, `FactoryRequest`, `FactoryResponse`, `ShipmentResponse`…)
étaient **déjà** des records. La moitié « DTO » de la leçon était donc acquise ; c'est la moitié
« entité » qui manquait. Le refactor n'a fait qu'appliquer aux entités le principe qui était déjà
appliqué aux DTO.

---

## 6. Une tension mise au jour — et volontairement laissée en place

La suppression des setters a fait remonter une question de conception qui était jusque-là
invisible. Elle mérite d'être regardée, **mais elle n'a pas été tranchée** : les contrats de
service et de contrôleur du projet sont restés identiques.

Les interfaces de service prennent une **entité** comme véhicule de données de requête :

```java
Store updateStore(Long storeId, Store store);
Factory patchFactory(Long factoryId, Factory request);
```

Le contrôleur fabrique donc, via `mapper.toEntity(request)`, un `Store` détaché et à moitié
rempli, dont le seul rôle est de transporter un nom et une adresse jusqu'au service. Ce `Store`
n'est pas un magasin : c'est un formulaire déguisé en entité. Tant que l'entité avait un
constructeur vide et des setters, personne ne pouvait le voir.

Le refactor l'a rendu visible par un point de friction très concret : **`@PatchMapping` n'a pas de
`@Valid`**. Un PATCH qui ne change que l'adresse arrive avec `name == null`. Si le constructeur de
création exigeait un nom non-blanc, `mapper.toEntity(request)` exploserait sur une requête
parfaitement légitime.

Deux issues possibles :

| | Ce que ça donne | Ce que ça coûte |
|---|---|---|
| **A.** Le service prend le record (`updateStore(Long, StoreRequest)`) | Le constructeur peut rester strict ; `toEntity` disparaît ; le flux devient « record entre, entité décide, record sort » | **Change le contrat de service et de contrôleur** — hors du périmètre d'un refactor d'entités |
| **B.** L'entité reste le porteur de requête | Contrats intacts, refactor confiné aux entités | Le constructeur doit tolérer les champs absents, donc il ne garantit plus la complétude |

**C'est B qui a été retenu ici**, et le constructeur le documente :

```java
/**
 * Valide ce qui est présent et tolère l'absent : cette entité sert aussi de porteur
 * de requête pour PATCH, où un champ null signifie "absent", pas "invalide".
 * Le caractère obligatoire des champs est porté par FactoryRequest (@NotBlank/@NotNull).
 */
public Factory(String name, Integer production) {
    if (name != null) this.name = requireName(name);
    if (production != null) this.production = requirePositive(production, "…");
    this.stock = 0;
}
```

La leçon à tirer n'est pas « B est le bon choix », c'est celle-ci : **un refactor d'entités ne doit
pas se transformer en refactor d'architecture au passage.** Le périmètre annoncé était les DAO ;
les contrats de service et de contrôleur n'en font pas partie. Quand un refactor commence à
déborder sur des signatures publiques, c'est le signal qu'on a trouvé une *deuxième* décision de
conception — qui se discute et se planifie séparément, pas qui se glisse dans la précédente.

La tension reste donc ouverte, documentée, et posée en exercice (§10-3).

Ce qui a bougé dans les mappers est minime et strictement mécanique : `address` étant `@Transient`
et privé de setter, `toEntity` le renseigne par la méthode d'intention.

```java
@AfterMapping
default void fillAddress(FactoryRequest request, @MappingTarget Factory factory) {
    factory.describeAs(request.address());
}
```

En revanche, les cinq `@Mapping(target = "…", ignore = true)` sur `id`, `stock`, `latitude`,
`longitude` et `version` ont pu **disparaître** : ces champs ne sont tout simplement plus
accessibles en écriture, donc MapStruct ne les considère même plus comme des cibles non mappées.
C'est la leçon en miniature : une contrainte exprimée par des **annotations qu'on peut oublier**
est devenue une contrainte portée par le **type**, qu'on ne peut plus contourner.

## 7. Le test comme révélateur

Les tests existants utilisaient `Factory.builder()` douze fois. Sans `@Builder`, ils ne compilent
plus. C'est une **bonne nouvelle**, et elle mérite d'être regardée de près :

```java
// AVANT — on INJECTE un état
Factory factory = Factory.builder().name("Marseille").production(7).stock(20).build();
factory.produce();
assertEquals(27, factory.getStock());
```

```java
// APRÈS — on PRODUIT l'état par des événements
Factory factory = new Factory("Marseille", 7);
factory.produce(20);
factory.produce();
assertEquals(27, factory.getStock());
```

Le test « avant » vérifie une arithmétique. Le test « après » raconte un scénario : une usine est
créée vide, produit 20 trombones, puis fait son tick de production. Le second teste le domaine ;
le premier testait un accumulateur.

Et surtout : le refactor a rendu possibles des tests qui étaient **inécrivables** avant, parce que
la règle n'était pas dans l'objet.

```java
@Test
void ship_moreThanStock_shouldThrowAndLeaveStockUntouched() {
    Factory factory = new Factory("Paris", 10);
    factory.produce();                                          // stock = 10
    assertThrows(ConflictException.class, () -> factory.ship(50));
    assertEquals(10, factory.getStock());                       // rien n'a bougé
}

@Test
void markDelivered_twice_shouldThrow() {
    Shipment shipment = inTransit(store);
    shipment.markDelivered(LocalDateTime.now());
    assertThrows(IllegalStateException.class, () -> shipment.markDelivered(LocalDateTime.now()));
    assertEquals(30, store.getStock(), "le stock ne doit pas avoir été crédité deux fois");
}
```

**Voilà le test décisif d'un bon découpage :** si une règle métier ne peut pas être testée sans
démarrer Spring, sans mocker un repository, sans passer par un service — c'est qu'elle n'est pas à
sa place. Les 15 tests de `FactoryTest`, `StoreTest` et `ShipmentTest` ne chargent aucun contexte
et s'exécutent en 35 millisecondes.

---

## 8. Ce que la leçon ne dit pas

Une règle qu'on enseigne sans ses limites devient un dogme. Trois nuances honnêtes.

**Tous les champs ne sont pas de l'état métier.** `address` est `@Transient` : c'est un libellé
d'affichage recalculé par géocodage inverse, pas une donnée du domaine. Il a gardé un mutateur —
mais nommé `describeAs()` et non `setAddress()`, et documenté comme non persisté. Le nom dit la
nature du champ. La vraie question n'est jamais « setter ou pas », c'est « ce champ est-il de
l'état, ou de la présentation ? ».

**L'exception levée par l'entité est un compromis assumé.** `Factory.ship()` lève
`ConflictException`, qui vit dans `configurations.exceptions` : le domaine dépend donc de la
couche web. Le bénéfice est immédiat (l'`ApiControllerHandler` la traduit en HTTP 409 sans rien
changer), le coût est un couplage. La version puriste serait une `InsufficientStockException` de
domaine, traduite par le handler. Ce choix est ouvert — le point de la leçon est **où vit la
règle**, pas quelle exception elle lève.

**Le constructeur ne garantit pas la complétude.** Parce que l'entité sert aussi de porteur de
requête PATCH (§6), `new Factory(null, null)` reste constructible. L'obligation des champs est
portée par `FactoryRequest` (`@NotBlank`, `@NotNull`) et par `@Column(nullable = false)`. Ce que
le constructeur garantit, c'est qu'aucune valeur *présente* n'est absurde, et que `id`, `version`
et `stock` ne sont jamais imposés de l'extérieur. Le cœur de la leçon — les invariants de stock et
les transitions d'état — n'est pas affecté.

**Ce n'est pas un chemin à sens unique universel.** Sur un CRUD sans aucune règle — une table de
référence, un paramétrage — un modèle anémique coûte moins cher qu'il ne rapporte, et Fowler
lui-même distingue « modèle de domaine » et « script de transaction ». La règle s'applique quand
il y a des invariants à tenir. Dans `clipper`, il y en a : `stock >= 0`, un statut qui ne recule
pas, une position qui est un couple.

---

## 9. Bilan mesuré

| | Avant | Après |
|---|---|---|
| Appels de setters dans `src/` | **26** | **0** |
| `@Setter` / `@Builder` / `@AllArgsConstructor` dans `models/dao/` | 9 annotations | **0** |
| Où vit la règle « stock suffisant » | dans `StoreServiceImp` | **dans `Factory.ship()`** |
| Mutations dans `DeliveryService.checkDeliveries()` | 3 lignes indépendantes | **1** transition atomique |
| Champs mutables depuis l'extérieur | tous, `id` et `version` compris | **aucun** |
| `@Mapping(ignore = true)` à maintenir | 10 | **0** |
| Contrats de service / contrôleur modifiés | — | **0** |
| Tests | 14 | **30** (dont 15 d'invariants purs, sans Spring ni mock) |

Diff : 12 fichiers modifiés et 3 nouveaux fichiers de test. Les interfaces `FactoryService`
et `StoreService` sont **inchangées**.
`mvn test` : **30 tests, 0 échec, 0 erreur**.

---

## 10. À retenir

1. **Le setter n'externalise pas la donnée, il externalise la raison.** Trois événements
   différents produisent le même `setStock(60)` : la cause est perdue, et avec elle la règle.
2. **Test du paramètre** — un setter reçoit **le résultat**, une intention reçoit **l'événement**.
   `setStock(60)` : l'appelant a déjà fait le calcul, donc il a déjà appliqué (ou oublié) la
   règle. `ship(40)` : l'entité calcule, donc elle peut refuser.
3. **Test de l'énoncé** — lis l'appel à voix haute à quelqu'un du métier. « L'usine expédie 40
   trombones » est une phrase ; « l'usine met son stock à 60 » n'en est pas une.
4. **Signal d'alarme** — si tu ne peux nommer la méthode qu'en citant le champ qu'elle modifie
   (`setStock`, `updateStock`, `addStock`), tu n'as pas trouvé l'intention, tu as trouvé un
   synonyme de setter.
5. **Une intention est indivisible** : elle porte tout ce que l'événement implique. Trois setters
   rendent les conséquences optionnelles ; une méthode les rend solidaires. On ne peut plus
   « livrer à moitié », parce que la phrase n'existe pas.
6. Un invariant vérifié **chez l'appelant** n'est pas garanti : il est espéré, autant de fois
   qu'il y a d'appelants. Un invariant vérifié **dans l'objet** est garanti une fois pour toutes.
7. Le meilleur bug est celui qu'on ne peut pas écrire. On n'ajoute pas de vérification : on
   **retire les moyens** de créer l'état interdit — `status` hors du constructeur, `id` et
   `version` non mutables.
8. **JPA n'a jamais demandé de setters.** Avec l'accès par champ, Hibernate les ignore ; la spec
   ne réclame qu'un constructeur no-arg `protected`. Il n'y a rien à troquer.
9. Deux verrous, un principe : le **record** t'empêche d'écrire l'interdit (le compilateur
   refuse) ; l'**entité** ne te donne pas les mots pour le dire (le vocabulaire est réduit aux
   gestes légaux).
10. Une règle qui exige un mock et un contexte Spring pour être testée n'est pas à sa place.

### Exercices

1. Dans `FactoryServiceImp.patchFactory`, les `if (request.x() != null)` subsistent. Est-ce une
   régression de la leçon, ou est-ce à sa place ? Justifier. *(Indice : le `null` de PATCH
   signifie « champ absent de la requête », pas « valeur du domaine ». C'est de la sémantique
   HTTP, pas du métier.)*
2. `Shipment.markDelivered()` appelle `store.receive()`. En DDD strict, un agrégat ne devrait pas
   en modifier un autre dans la même transition. Identifier le risque, et proposer l'alternative.
3. Reprendre le tableau A/B du §6. Basculer le projet en **A** : les services prennent
   `StoreRequest` / `FactoryRequest`, `toEntity` disparaît, les constructeurs redeviennent stricts.
   Mesurer ce que ça change dans les contrôleurs et les tests, puis argumenter : est-ce que le
   gain de sûreté justifie de casser trois signatures publiques ?
4. Extraire une `InsufficientStockException` de domaine et la faire traduire par
   `ApiControllerHandler`. Qu'est-ce que ça améliore, et qu'est-ce que ça coûte ?
5. Appliquer les deux tests du §1.3 et §1.4 à ces signatures, et proposer un meilleur nom quand
   il en faut un : `setDeliveredAt(LocalDateTime)`, `updateCoordinates(double, double)`,
   `addStock(int)`, `setProduction(int)`, `describeAs(String)`. *(Attention : l'une d'elles est
   déjà correcte — laquelle, et pourquoi ?)*

---

## Sources

- **Martin Fowler**, *AnemicDomainModel*, 25 novembre 2003 — https://martinfowler.com/bliki/AnemicDomainModel.html
- **Eric Evans**, *Domain-Driven Design* (2003), ch. 6 — Aggregates & invariants
- *Anemic domain model* — synthèse, critiques et réponses DDD — https://en.wikipedia.org/wiki/Anemic_domain_model
- **codecentric**, *DDD vs. Anemic Domain Models* — https://www.codecentric.de/en/knowledge-hub/blog/ddd-vs-anemic-domain-models
- **Hibernate ORM User Guide**, *Access strategies* — https://docs.jboss.org/hibernate/orm/5.1/userguide/html_single/chapters/domain/access.html
- **Thorben Janssen**, *Access Strategies in JPA and Hibernate* — https://thorben-janssen.com/access-strategies-in-jpa-and-hibernate/
- **Vlad Mihalcea**, *Fluent API entity building with JPA and Hibernate* — https://vladmihalcea.com/fluent-api-entity-building-with-jpa-and-hibernate/
- **OpenJDK**, *JEP 395: Records* — https://openjdk.org/jeps/395
- **Gunnar Morling**, *Enforcing Java Record Invariants With Bean Validation* — https://www.morling.dev/blog/enforcing-java-record-invariants-with-bean-validation/
- **Joshua Bloch**, *Effective Java* (3e éd.), Item 17 « Minimize mutability »
