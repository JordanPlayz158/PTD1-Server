<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Database Migration?</title>
    <link rel="stylesheet" href="/assets/bootstrap/bootstrap.css">
    <script src="/assets/bootstrap/bootstrap.js"></script>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap5-toggle@5.0.6/css/bootstrap5-toggle.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap5-toggle@5.0.6/js/bootstrap5-toggle.ecmas.min.js"></script>

    <style>
        /* Chrome, Safari, Edge, Opera */
        input::-webkit-outer-spin-button,
        input::-webkit-inner-spin-button {
         -webkit-appearance: none;
         margin: 0;
        }

        /* Firefox */
        input[type=number] {
         -moz-appearance: textfield;
        }
    </style>
    <script>
        function onQuestionToggle(event) {
          document.getElementById("databaseDetails").style.display = event.target.checked ? 'unset' : 'none'
        }
    </script>
    <#if totalRows??>
        <script>
            document.addEventListener("DOMContentLoaded", function(event) {
                const migrationMessage = document.getElementById("migrationMessage");
                const migrationProgress = document.getElementById("migrationProgress");
                const totalRowsElement = document.getElementById("totalRows");
                const currentRowElement = document.getElementById("currentRow");
                const totalRows = parseInt(totalRowsElement.textContent.replaceAll(',', ''));

                var interval = setInterval(async function() {
                    try {
                        const response = await fetch(`/progress`, {
                            method: "POST",
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded',
                            },

                            body: "progress=",
                        });

                        if(!response.ok) {
                            clearInterval(interval);
                            return;
                        }

                        const currentRowString = await response.text();
                        const currentRow = parseInt(currentRowString);

                        if(isNaN(currentRow)) {
                            migrationMessage.className = "alert alert-danger text-center";
                            migrationMessage.textContent = currentRowString;
                            migrationProgress.className = "progress-bar progress-bar-striped bg-danger"
                            clearInterval(interval);
                            return;
                        }

                        currentRowElement.textContent = currentRow.toLocaleString();
                        migrationProgress.style.width = ((currentRow / totalRows) * 100) + "%";

                        if(currentRow === totalRows) {
                            migrationMessage.className = "alert alert-success text-center";
                            migrationMessage.textContent = "Migration Successful! Manual Restart Required.";
                            migrationProgress.className = "progress-bar progress-bar-striped bg-success"
                            clearInterval(interval);
                            return;
                        }
                    } catch (error) {
                        migrationMessage.className = "alert alert-danger text-center";
                        migrationMessage.textContent = error.message;
                        migrationProgress.className = "progress-bar progress-bar-striped bg-danger"
                        clearInterval(interval);
                    }
                }, 1000);
            });
        </script>
    </#if>
</head>
<body>
<div class="container pt-5">
    <#if totalRows??>
        <div id="migrationMessage" class="alert alert-primary text-center" style="white-space: pre-line;" role="alert">
            Migrating...
        </div>
    </#if>
    <#list reasons![] as reason>
        <div class="alert alert-danger text-center" role="alert">
            ${reason}
        </div>
    </#list>
    <form method="post" id="migrationForm" class="text-center" >
        <div class="mb-3">
            <#if !totalRows??>
                <label for="dbMigrationQuestion" class="form-label">Do you wish to migrate an old Pokemon Tower Defense 1 (PTD1) Database?</label>
                <div id="dbMigrationQuestionHelp" class="form-text mb-2">This only applies to databases that have the latest laravel-10 database schema!</div>
                <input type="checkbox" class="form-control" id="dbMigrationQuestion" name="isMigrating" data-size="lg" data-toggle="toggle" data-onlabel="Yes" data-offlabel="No" data-onstyle="success" data-offstyle="danger" onchange="javascript:onQuestionToggle(event)" autocomplete="off" checked>
            <#else>
                <label for="dbMigrationQuestion" class="form-label">Do you wish to cancel the migration?</label>
                <div id="dbMigrationQuestionHelp" class="form-text mb-2">No migration data will be in the new database if migration is stopped!</div>
                <input type="checkbox" class="form-control" id="dbMigrationQuestion" data-size="lg" data-toggle="toggle" data-onlabel="Yes" data-offlabel="No" data-onstyle="success" data-offstyle="danger" onchange="javascript:onQuestionToggle(event)" autocomplete="off" checked disabled>
            </#if>
        </div>
        <div id="databaseDetails" <#if totalRows??>style="display: none"</#if>>
            <div class="mb-3">
                <label for="databaseHost" class="form-label">Host</label>
                <input type="text" class="form-control text-center" id="databaseHost" name="host"/>
            </div>
            <div class="mb-3">
                <label for="databasePort" class="form-label">Port</label>
                <input type="number" class="form-control text-center" id="databasePort" name="port" value="3306"/>
            </div>
            <div class="mb-3">
                <label for="databaseName" class="form-label">Name</label>
                <input type="text" class="form-control text-center" id="databaseName" name="name"/>
            </div>
            <div class="mb-3">
                <label for="databaseUsername" class="form-label">Username</label>
                <input type="text" class="form-control text-center" id="databaseUsername" name="username"/>
            </div>
            <div class="mb-3">
                <label for="databasePassword" class="form-label">Password</label>
                <input type="password" class="form-control text-center" id="databasePassword" name="password"/>
            </div>
        </div>
        <#if totalRows??>
            <div class="mb-3">
                <label for="migrationProgress" class="form-label">Migration Progress</label>
                <div id="migrationProgressHint" class="form-text mb-2"><span id="currentRow">0</span> / <span id="totalRows">${totalRows!0}</span> rows</div>
                <div class="progress">
                    <div id="migrationProgress" class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" style="width: 0%"></div>
                </div>
            </div>
        </#if>
        <button type="submit" class="btn btn-primary">Submit</button>
    </form>
</div>
</body>
</html>