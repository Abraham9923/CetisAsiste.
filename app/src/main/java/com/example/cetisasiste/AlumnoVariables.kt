package com.example.cetisasiste

/*
 * VARIABLES de las vistas de alumno (res/layout/alumno_*.xml).
 *
 * Aquí NO hay datos: solo se declara QUÉ información necesita cada vista.
 * Quien tenga los datos (por ejemplo, una consulta a Firestore) crea estos
 * objetos y se los pasa a AlumnoVistas, que los coloca en la pantalla.
 *
 * Todos los campos tienen valor vacío por defecto. Así Firestore puede
 * convertir un documento directamente:  documento.toObject(Alumno::class.java)
 * (los nombres coinciden con los campos de la colección "usuarios"; son `var`
 * porque Firestore necesita poder escribirlos).
 */

/** Datos del alumno que se muestran en Inicio, Perfil, Código QR y Credencial. */
data class Alumno(
    var nombre: String = "",
    var rol: String = "",
    var grupo: String = "",
    var noControl: String = "",
    var correo: String = "",
    /** Ciclo escolar de la credencial. */
    var vigencia: String = "",
)

/** Una fila de la pantalla Horario. */
data class Clase(
    var materia: String = "",
    /** Rango de horas, tal como se mostrará. */
    var horario: String = "",
    /** Texto después del punto medio (profesor, aula, nota…). */
    var detalle: String = "",
)

/** Días que aparecen como chips en Horario. */
enum class DiaSemana(val nombre: String) {
    LUNES("lunes"),
    MARTES("martes"),
    MIERCOLES("miércoles"),
    JUEVES("jueves"),
    VIERNES("viernes"),
}

/** Todo lo que necesita la pantalla Horario. */
data class HorarioDelDia(
    val grupo: String = "",
    val dia: DiaSemana = DiaSemana.LUNES,
    val clases: List<Clase> = emptyList(),
)

/** Una tarjeta del Boletín de noticias. */
data class Anuncio(
    var etiqueta: String = "",
    var fecha: String = "",
    var titulo: String = "",
    var texto: String = "",
)
