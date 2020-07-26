const PROTOCOL_HTTP = "http";
const PROTOCOL_WEBSOCKET = "websocket";

const NOTI_LEVEL_INFO = "info";
const NOTI_LEVEL_WARNING = "warning";
const NOTI_LEVEL_ERROR = "error";

let toast = $("#toast-div .toast");
let sdgCanvas = $("#sdg-canvas");
let spcCanvas = $("#spc-canvas");
let showControlChart = $("#graph-show-control-chart");
let spcTypesInput = $('#spcGraphOptionsMenuLink').parent().find("input[name='controlChartType']");
let notificationsDropdown = $("#notificationsMenuLink").parent().find(".dropdown-menu");
let stompClient = null;

$('.tip').tooltip();

$(document).ready( function () {
    fetch("/web-page/system-names")
        .then(response => response.json())
        .then(systems => {
            let menu = $("#systemsDropdownMenu");
            Object.values(systems).forEach(systemName => {
                let sysButton = $("<button></button>")
                    .attr("class", "dropdown-item")
                    .attr("value", systemName)
                    .attr("onclick", "startSDGGraph(this)")
                    .append(systemName);
                menu.append(sysButton);
            });
        });
    connectSocket();
    requestNotificationPermission();
});

function connectSocket() {
    let socket = new SockJS("/mgp-websocket");
    stompClient = Stomp.over(socket);
    stompClient.reconnect_delay = 5000;
    stompClient.connect({}, (frame) => {
        toast.find(".toast-header")
            .attr("class", "toast-header text-white bg-primary")
            .prepend("<i class='fas fa-info-circle mr-2'></i>");
        toast.find("strong").empty().append("Connected");
        toast.find(".toast-body").empty().append("Successfully connected to the MGP service!");
        toast.toast('show');
        console.log("Connected: " + frame);
    });
}

function requestNotificationPermission() {
    if (Notification && Notification.permission !== "granted") {
        Notification.requestPermission(function (status) {
            if (Notification.permission !== status) {
                Notification.permission = status;
            }
        })
    }
}

let addInlineStyle = function(children) {
    for (let i = 0; i < children.length; i++) {
        let child = children[i];
        if (child instanceof Element) {
            let cssText = '';
            let computedStyle = window.getComputedStyle(child, null);
            for (let i = 0; i < computedStyle.length; i++) {
                let prop = computedStyle[i];
                cssText += prop + ':' + computedStyle.getPropertyValue(prop) + ';';
            }
            child.setAttribute('style', cssText);
            addInlineStyle(child.childNodes);
        }
    }
};

let downloadGraphLink =  $("#download-graph");

downloadGraphLink.ready = false;
downloadGraphLink.click(function () {
    if (!downloadGraphLink.ready) {
        event.preventDefault();
        sdgGraph.stopSimulation();
        let svg = document.querySelector("#sdg-canvas").cloneNode(true);
        document.body.appendChild(svg);
        addInlineStyle(svg.childNodes);
        html2canvas(svg).then(canvas => {
            svg.remove();
            sdgGraph.restartSimulation();
            downloadGraphLink.attr("href", canvas.toDataURL("image/png"), 1.0);
            let dt = new Date();
            downloadGraphLink.attr("download", "SDG_" + startSDGGraph.systemName + "_" +  dt.getFullYear() + "-" + dt.getMonth() + "-" + dt.getDate() + ".png");
            downloadGraphLink.ready = true;
            this.click();
        });
    } else {
        downloadGraphLink.ready = false;
    }
});

function exportSVG() {
    const svg = document.querySelector("#graph").cloneNode(true);
    document.body.appendChild(svg);
    const g = svg.querySelector("g");
    svg.setAttribute("width", g.getBBox().width);
    svg.setAttribute("height", g.getBBox().height);
    const svgAsXML = (new XMLSerializer).serializeToString(svg);
    const svgData = `data:image/svg+xml,${encodeURIComponent(svgAsXML)}`;
    downloadGraphLink.attr("href", svgData);
    downloadGraphLink.attr("download", "graph.svg");
}

let subscribeSdgGraph = null;
let subscribeNotify = null;
let sdgGraph = null;

let subscribeSpcGraph = null;
let spcGraph = null;
let spcStartProcess = null;

function startSDGGraph(systemName) {
    startSDGGraph.systemName = systemName.value;

    $("#systemsDropdownMenuButton")
        .text(systemName.value);

    $("#system-options-menu-button").prop("disabled", false);

    $("#systemsDropdownMenu button.active")
        .removeClass("active")
        .css("pointer-events", "auto");

    $(systemName).addClass("active")
        .css("pointer-events", "none");

    // If graph exist, unsubscribe and clear graph.
    if (subscribeSdgGraph !== null && sdgGraph !== null) {
        subscribeSdgGraph.unsubscribe();
        sdgGraph.closeNodeCard();
        sdgGraph = null;
        $("#graph g").remove();
    }

    if (subscribeNotify !== null) {
        subscribeNotify.unsubscribe();
    }

    // Subscribe graph topic
    subscribeSdgGraph = stompClient.subscribe("/topic/graph/" + systemName.value, function (message) {
        console.log("message： " + message);
        let data = JSON.parse(message.body);
        if (sdgGraph === null) {
            sdgGraph = new SDGGraph(data);
            $(sdgGraph).on('selectNode', function (event, node) {
                if (showControlChart[0].checked) {
                    let setting = getSpcSetting();
                    if (setting) {
                        if (setting.target === "app") {
                            if (node.labels.includes("Service")) {
                                if (spcStartProcess) {
                                    spcStartProcess.stop();
                                }
                                if (spcCanvas.hasClass("collapse")) {
                                    sdgCanvas.addClass("split-up");
                                    spcCanvas.addClass("split-down");
                                    spcCanvas.removeClass("collapse");
                                    window.dispatchEvent(new Event('resize'));
                                }
                                spcStartProcess = new StartSPCGraph(startSDGGraph.systemName, setting.type, setting.protocol);
                            } else {
                                spcCanvas.addClass("collapse");
                                sdgCanvas.removeClass("split-up");
                                spcCanvas.removeClass("split-down");
                                spcGraph = null;
                                window.dispatchEvent(new Event('resize'));
                                spcCanvas.empty();
                            }
                        }
                    }
                }
            });
        } else {
            sdgGraph.updateData(data);
        }
    });

    let notificationCount = 0;

    function createNotificationDropdownItem(notification) {
        let headerClass = "toast-header ";
        let icon;
        if (notification.level === NOTI_LEVEL_INFO) {
            headerClass += "text-white bg-primary";
            icon = "<i class='fas fa-info-circle mr-2'></i>";
        } else if (notification.level === NOTI_LEVEL_WARNING) {
            headerClass += "text-white bg-warning";
            icon = "<i class='fas fa-exclamation-triangle mr-2'></i>";
        } else if (notification.level === NOTI_LEVEL_ERROR) {
            headerClass += "text-white bg-danger";
            icon = "<i class='fas fa-bug mr-2'></i>";
        }

        return "<button class='dropdown-item'>" +
            "<div class='toast show'>" +
            "<div class='" + headerClass + "'>" +
            icon +
            "<strong class='mr-auto'>" + notification.title + "</strong>" +
            "<small>" + notification.dateTime + "</small>" +
            "</div>" +
            "<div class='toast-body'>" + notification.htmlContent + "</div>" +
            "</div>" +
            "</button>";
    }

    function removeNotificationDropdownItem() {
        if (notificationCount > 100) {
            notificationsDropdown.find(".dropdown-item:gt(99)").remove();
        }
    }

    // Fetch current system notifications
    fetch("/web-page/notification/" + systemName.value)
        .then(response => response.json())
        .then(notifications => {
            notificationCount += notifications.length;
            notificationsDropdown.empty();
            notifications.forEach(notification => {
                let item = $(createNotificationDropdownItem(notification));
                if (notification.appName && notification.version) {
                    item.click(function () {
                        sdgGraph.clickNodeByNameAndVersion(notification.appName, notification.version)
                    });
                }
                notificationsDropdown.append(item);
            });
        });

    // Subscribe notification topic
    subscribeNotify = stompClient.subscribe("/topic/notification/" + systemName.value, function (message) {
        let data = JSON.parse(message.body);
        notificationCount++;
        toastNotification(data);
        desktopNotification(data);
    });

    function toastNotification(data) {
        toast.find("i").remove();
        if (data.level === NOTI_LEVEL_INFO) {
            toast.find(".toast-header")
                .attr("class", "toast-header text-white bg-primary")
                .prepend("<i class='fas fa-info-circle mr-2'></i>");
        } else if (data.level === NOTI_LEVEL_WARNING) {
            toast.find(".toast-header")
                .attr("class", "toast-header text-white bg-warning")
                .prepend("<i class='fas fa-exclamation-triangle mr-2'></i>");
        } else if (data.level === NOTI_LEVEL_ERROR) {
            toast.find(".toast-header")
                .attr("class", "toast-header text-white bg-danger")
                .prepend("<i class='fas fa-bug mr-2'></i>");
        }
        toast.find("strong").empty().append(data.title);
        toast.find(".toast-body").empty().append(data.htmlContent);
        toast.toast('show');

        let item = $(createNotificationDropdownItem(data));
        if (data.appName && data.version) {
            item.click(function () {
                sdgGraph.clickNodeByNameAndVersion(data.appName, data.version)
            });
        }
        notificationsDropdown.prepend(item);
        removeNotificationDropdownItem();
    }

    function desktopNotification(data) {
        if (Notification.permission === "granted") {
            let n = new Notification(data.content, {tag: data.title});
            n.onclick = function () {
                sdgGraph.clickNodeByNameAndVersion(data.appName, data.version);
            }
        }
    }


    // monitorErrorsChart
    fetch("/web-page/monitor/getErrorChart/" + systemName.value)
        .then(response => response.json())
        .then(json => {

            let jsonContent = json["map"];
            let labels = [];
            let datas = [];

            for(let key in jsonContent){
                labels.push(key);
            }
            labels.sort(function(a, b) {
                return new Date(a) - new Date(b);
            });

            labels.forEach(arrKey => {
                for(let key in jsonContent) {
                    if(arrKey === key){
                        datas.push(jsonContent[key]);
                        break;
                    }
                }
            });


            let ctx = document.getElementById('monitorErrorsChart').getContext('2d');

            let config = {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'MonitorErrorNum',
                        backgroundColor: 'rgba(255, 99, 132, 0.5)',
                        borderColor: 'rgba(255,99,132,1)',
                        borderWidth: 5,
                        data: datas,
                        fill: false,
                        fontSize: 30
                    }]
                },
                options: {
                    responsive: true,
                    title: {
                        display: true,
                        text: 'MonitorErrorsChart',
                        fontSize: 30
                    },
                    scales: {
                        xAxes: [{
                            gridLines: {
                                offsetGridLines: true
                                // drawOnChartArea: true
                            },
                            ticks: {
                                fontSize: 20
                            }
                        }],
                        yAxes: [{
                            gridLines: {
                                drawBorder: false
                                // color: ['pink', 'red', 'orange', 'yellow', 'green', 'blue', 'indigo', 'purple']
                            },
                            ticks: {
                                min: 0,
                                max: 40,
                                stepSize: 10,
                                fontSize: 20
                            }
                        }]
                    }
                }
            };

            let myChart = new Chart(ctx, config);
        });


    // RiskPositivelyCorrelatedChart
    fetch("/web-page/monitor/getRiskPositivelyCorrelatedChart/" + systemName.value)
        .then(response => response.json())
        .then(json => {

            let jsonContent_servicesErrorNum = json["servicesErrorNum"];
            let labels_servicesErrorNum = [];
            let datas_servicesErrorNum = [];
            let jsonContent_risk = json["risk"];
            let labels_risk = [];
            let datas_risk = [];

            for(let key in jsonContent_servicesErrorNum){
                labels_servicesErrorNum.push(key.split(":")[1] + ":" + key.split(":")[2]);
                datas_servicesErrorNum.push(jsonContent_servicesErrorNum[key]);
            }
            for(let key in jsonContent_risk){
                labels_risk.push(key.split(":")[1] + ":" + key.split(":")[2]);
                datas_risk.push(Math.round(jsonContent_risk[key] * 1000) / 1000);
            }

            let ctx = document.getElementById('RiskPositivelyCorrelatedChart').getContext('2d');
            // 多線
                let config = {
                    type: 'line',
                    data: {
                        labels: labels_servicesErrorNum,
                        datasets: [{
                            label: 'servicesErrorNum',
                            yAxisID: 'servicesErrorNum',
                            backgroundColor: 'rgba(119,9,10,0.78)',
                            borderColor: 'rgba(119,9,10,0.78)',
                            borderWidth: 5,
                            data: datas_servicesErrorNum,
                            fill: false
                        }, {
                            label: 'risk',
                            yAxisID: 'risk',
                            backgroundColor: 'rgba(121,192,54,0.77)',
                            borderColor: 'rgba(121,192,54,0.77)',
                            borderWidth: 5,
                            data: datas_risk,
                            fill: false,
                        }]
                    },
                    options: {
                        responsive: true,
                        title: {
                            display: true,
                            text: 'RiskPositivelyCorrelatedChart',
                            fontSize: 30
                        },
                        scales: {
                            xAxes: [{
                                gridLines: {
                                    offsetGridLines: true
                                },
                                ticks: {
                                     fontSize: 15
                                }
                            }],
                            yAxes: [
                                {
                                    id: 'servicesErrorNum',
                                    type: 'linear',
                                    position: 'left',
                                    ticks: {
                                        min: 0,
                                        max: 80,
                                        stepSize: 10,
                                        fontSize: 20,
                                        fontColor: 'rgba(119,9,10,0.78)'
                                    }
                                }, {
                                    id: 'risk',
                                    type: 'linear',
                                    position: 'right',
                                    ticks: {
                                        min: 0,
                                        max: 1.6,
                                        stepSize: 0.2,
                                        fontSize: 20,
                                        fontColor: 'rgba(121,192,54,0.77)'
                                    }
                                }
                            ]
                        }
                    }
                };

            let myChart = new Chart(ctx, config);
        });

    stompClient.send("/mgp/graph/" + systemName.value);
}

function getSpcSetting() {
    let setting;
    spcTypesInput.each(function () {
        if (this.checked) {
            setting = {};
            setting.type = this.value;
            setting.protocol = $(this).data("protocol");
            setting.target = $(this).data("target");
            return false;
        }
    });
    return setting;
}

showControlChart.on("change", function () {
    if (spcStartProcess) {
        spcStartProcess.stop();
    }
    if (this.checked) {
        let setting = getSpcSetting();
        if (setting) {
            if ((setting.target === "app" && sdgGraph.selectedNode) || setting.target === "apps") {
                sdgCanvas.addClass("split-up");
                spcCanvas.addClass("split-down");
                spcCanvas.removeClass("collapse");
                window.dispatchEvent(new Event('resize'));
                spcStartProcess = new StartSPCGraph(startSDGGraph.systemName, setting.type, setting.protocol);
            }
        }
    } else {
        spcCanvas.addClass("collapse");
        sdgCanvas.removeClass("split-up");
        spcCanvas.removeClass("split-down");
        window.dispatchEvent(new Event('resize'));
        spcGraph = null;
        spcCanvas.empty();
    }

});

spcTypesInput.on("change", function () {
    if (spcStartProcess) {
        spcStartProcess.stop();
    }
    if (showControlChart[0].checked) {
        let setting = getSpcSetting();
        if (setting && ((setting.target === "app" && sdgGraph.selectedNode) || setting.target === "apps")) {
            sdgCanvas.addClass("split-up");
            spcCanvas.addClass("split-down");
            spcCanvas.removeClass("collapse");
            window.dispatchEvent(new Event('resize'));
            spcStartProcess = new StartSPCGraph(startSDGGraph.systemName, setting.type, setting.protocol);
        }
    }
});

function StartSPCGraph(systemName, type, protocol) {
    let interval;
    if (subscribeSpcGraph !== null) {
        subscribeSpcGraph.unsubscribe();
    }

    if (spcGraph !== null) {
        spcGraph = null;
        spcCanvas.empty();
    }

    if (protocol === PROTOCOL_WEBSOCKET) {
        subscribeSpcGraph = stompClient.subscribe("/topic/graph/spc/" + type + "/" + systemName, function (message) {
            let data = JSON.parse(message.body);
            initOrUpdateSpcGraph(data);
        });
        stompClient.send("/mgp/graph/spc/" + type + "/" + systemName);
    } else if (sdgGraph.selectedNode.labels.includes("Service")) {
        let appId = sdgGraph.selectedNode.appId;
        fetch("/web-page/app/spc/" + type + "/" + appId)
            .then(response => response.json())
            .then(data => {
                initOrUpdateSpcGraph(data);
            });
        interval = setInterval(function () {
            fetch("/web-page/app/spc/" + type + "/" + appId)
                .then(response => response.json())
                .then(data => {
                    initOrUpdateSpcGraph(data);
                });
        }, 10000);
    }

    function initOrUpdateSpcGraph(data) {
        if (spcGraph === null) {
            spcGraph = new SPCGraph(spcCanvas.prop('id'), data);
        } else {
            spcGraph.updateData(data);
        }
    }

    this.stop = function () {
        if (interval) {
            clearInterval(interval);
        }
    }

}

