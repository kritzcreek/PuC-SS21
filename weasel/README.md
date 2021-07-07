# PrettyJSON Templatesprache

Das hier wird unser Projekt für PuC im Sommerstemester 2021. Wir wissen noch nicht wie weit wir kommen, alles sehr experimentell.

## Ziele

Die Sprache zielt darauf ab, JSON um ein paar features zu erweitern und die Arbeit damit für uns angenehmer zu gestalten.

- [x] Mathe innerhalb von JSON
- [ ] Let Bindings
  ```
  let x = {
      foo: "bar"
  }
  
  {
    x
  }
  => Evaluates to valid JSON
  ```

- [ ] Keine Gänsefüßchen (und evtl keine Kommas) mehr
  ```
  {
    field: "value"
    fieldTwo: 100
  }
  ```

- [ ] Kommentare
- [ ] Newlines (eventuell als Seperator)
- [ ] Building Block Functions
  ```kt
  fun a(foo: string) = 
  { 
    hallo: foo
    welt: "bar"
  }
  ```

- [ ] Repeat Blocks (if we're crazy enough)
  ```yaml
  helix:
      beat: 10
      repeat: 4
      startRotation: 90*repeat
  ```
- [X] Leading Kommas


![duckdance.gif](https://cdn.discordapp.com/emojis/853294931472941136.gif?v=1)
