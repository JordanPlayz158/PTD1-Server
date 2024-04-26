const page = document.documentElement

window.addEventListener('load', function () {
    const firstTheme = Cookies.get('theme')

    if (firstTheme !== undefined && firstTheme === 'dark') {
        setTheme('dark')
    } else {
        setTheme('light')
    }
})

function modeToggle() {
    setTheme(isLight(page.getAttribute('data-bs-theme')) ? 'dark' : 'light')
}

function setTheme(theme) {
    page.setAttribute('data-bs-theme', theme)
    newIcon(theme)
    newBackground(theme)

    Cookies.set('theme', theme, { expires: 365 })
}

function newIcon(theme) {
    const iconClasses = document.getElementById('toggleIcon').classList

    if (isLight(theme)) {
        iconClasses.replace('bi-sun', 'bi-moon')
    } else {
        iconClasses.replace('bi-moon', 'bi-sun')
    }
}

function newBackground(theme) {
    const background = 'bg-' + theme
    const navClasses = document.getElementById('nav').classList
    const navDivClasses = document.getElementById('navDiv').classList

    const footerClasses = document.getElementById('footer').classList


    if (isLight(theme)) {
        navClasses.replace('bg-dark', background)
        navDivClasses.replace('bg-dark', background)
        footerClasses.replace('bg-dark', background)
    } else {
        navClasses.replace('bg-light', background)
        navDivClasses.replace('bg-light', background)
        footerClasses.replace('bg-light', background)
    }
}

function isLight(theme) {
    return theme === "light"
}