document.addEventListener('DOMContentLoaded', function () {
    const menuToggle = document.getElementById('menu-toggle');
    const closeMenu = document.getElementById('close-menu');
    const mobileMenu = document.getElementById('mobile-menu');

    menuToggle.addEventListener('click', function () {
        mobileMenu.classList.remove('translate-x-full'); // Muestra el menú
    });

    closeMenu.addEventListener('click', function () {
        mobileMenu.classList.add('translate-x-full'); // Oculta el menú
    });

    // Cerrar el menú si se hace clic fuera de él
    mobileMenu.addEventListener('click', function (event) {
        if (event.target === mobileMenu) {
            mobileMenu.classList.add('translate-x-full');
        }
    });
});