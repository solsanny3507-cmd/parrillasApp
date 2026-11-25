function mostrarAlerta(mensaje) {
    Swal.fire({
        title: 'Registro Exitoso',
        text: mensaje,
        icon: 'success'
    });
}

function mostrarMensaje(icon,title,mensaje) {
    console.log(icon,title,mensaje);
    Swal.fire({
        title: title,
        text: mensaje,
        icon: icon
    });
}

function mostrarConfirmacion(referencia, url) {
    Swal.fire({
        title: "Transacción pendiente",
        html: `¿Desea continuar con la transacción?<br> <b>${referencia}</b>`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Sí, continuar",
        cancelButtonText: "Cancelar"
    }).then((result) => {
        if (result.isConfirmed) {
            // 🔥 redirección en la misma pestaña
            window.location.href = url;
        }
    });
}

function mostrarAdvertencia(title,mensaje) {
    Swal.fire({
        title: title,
        text: mensaje,
        icon: 'warning'
    });
}

function mostrarError(title,mensaje) {
    Swal.fire({
        title: title,
        text: mensaje,
        icon: 'error'
    });
}

