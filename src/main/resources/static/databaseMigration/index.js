window.addEventListener('load', function () {
    const databaseDetailsStyle = document.getElementById('databaseDetails').style

    document.getElementById("dbMigrationQuestion").onchange = onQuestionToggle
    function onQuestionToggle(e) {
        databaseDetailsStyle.setProperty('display',
            e.target.checked ? 'flex' : 'none',
            'important')
    }

    const totalRowsElement = document.getElementById('totalRows')

    if (totalRowsElement) {
        const migrationMessage = document.getElementById('migrationMessage')
        const migrationProgress = document.getElementById('migrationProgress')

        const currentRowElement = document.getElementById('currentRow')
        const totalRows = parseInt(totalRowsElement.textContent.replaceAll(',', ''))

        const interval = setInterval(async function () {
            try {
                const response = await fetch('progress')

                if (response.redirected) {
                    currentRowElement.textContent = totalRows.toLocaleString()
                    migrationProgress.style.width = '100%'
                    migrationMessage.classList.replace('alert-primary', 'alert-success')
                    migrationMessage.textContent = 'Migration Successful! Redirecting in 5 Seconds'
                    migrationProgress.classList.add('bg-success')

                    setTimeout(function () {
                        location.href = response.headers.get('Location')
                    }, 5000)

                    clearInterval(interval)
                    return
                }

                const currentRowString = await response.text()
                const currentRow = parseInt(currentRowString)

                if (isNaN(currentRow)) {
                    migrationMessage.classList.replace('alert-primary', 'alert-danger')
                    migrationMessage.textContent = currentRowString
                    migrationProgress.classList.add('bg-danger')
                    clearInterval(interval)
                    return
                }

                currentRowElement.textContent = currentRow.toLocaleString()
                migrationProgress.style.width = ((currentRow / totalRows) * 100) + '%'
            } catch (error) {
                migrationMessage.classList.replace('alert-primary', 'alert-danger')
                migrationMessage.textContent = error.message
                migrationProgress.classList.add('bg-danger')
                clearInterval(interval)
            }
        }, 1000)
    }
})