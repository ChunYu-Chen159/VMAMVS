function SDGGraph(data) {
    const LABEL_SERVICE = "Service";
    const LABEL_NULLSERVICE = "NullService";
    const LABEL_ENDPOINT = "Endpoint";
    const LABEL_NULLENDPOINT = "NullEndpoint";
    const LABEL_QUEUE = "Queue";
    const LABEL_OUTDATEDVERSION = "OutdatedVersion";
    const LABEL_HEAVY_STRONG_UPPER_DEPENDENCY = "HeavyStrongUpperDependency";
    const LABEL_HEAVY_STRONG_LOWER_DEPENDENCY = "HeavyStrongLowerDependency";
    const LABEL_HEAVY_WEAK_UPPER_DEPENDENCY = "HeavyWeakUpperDependency";
    const LABEL_HEAVY_WEAK_LOWER_DEPENDENCY = "HeavyWeakLowerDependency";

    const REL_OWN = "OWN";
    const REL_HTTPREQUEST = "HTTP_REQUEST";
    const REL_AMQPPUBLISH = "AMQP_PUBLISH";
    const REL_AMQPSUBSCRIBE = "AMQP_SUBSCRIBE";
    const REL_NEWERPATCHVERSION = "NEWER_PATCH_VsyERSION";

    const REL_TEXT_REL_HTTPREQUEST = "HTTP-REQUEST";
    const REL_TEXT_AMQPPUBLISH = "AMQP-PUBLISH";
    const REL_TEXT_AMQPSUBSCRIBE = "AMQP-SUBSCRIBE";
    const REL_TEXT_NEWERPATCHVERSION = "NEWER-PATCH-VERSION";

    const SYMBOL_SERVIVE = d3.symbolSquare;
    const SYMBOL_ENDPOINT = d3.symbolCircle;
    //const SYMBOL_ENDPOINT = d3.symbolCross;
    const SYMBOL_QUEUE = d3.symbolHexagonAlt;

    const SIZE_SERVIVE = 4500;
    const SIZE_ENDPOINT = 1500;
    const SIZE_QUEUE = SIZE_SERVIVE;

    const COLOR_NULL = "#3a3a3a";
    const COLOR_QUEUE = "#85d18c";
    const COLOR_WARNING = "orange";

    const HIGHLIGHT_LEVEL_NORMAL = "highlight";
    const HIGHLIGHT_MONITORERROR = "highlight_error";
    const HIGHLIGHT_MONITORERROR_SOURCE = "highlight_error_source";
    const HIGHLIGHT_LEVEL_WARNING = "warning";
    const HIGHLIGHT_LEVEL_ERROR = "error";
    const HIGHLIGHT_CONTRACTTESTING_WARNING = "contractWarning";
    const HIGHLIGHT_HIGHRISK_TRUE = "highRiskTrue";
    const HIGHLIGHT_MONITORERROR_TRUE = "monitorErrorTrue";


    const CONDITION_CONTRACTTEST_PASS = "PASS";
    const CONDITION_CONTRACTTEST_WARNING = "WARNING";

    const CONDITION_HIGHRISK_TRUE = "TRUE";
    const CONDITION_HIGHRISK_FALSE = "FALSE";

    const CONDITION_MONITORERROR_TRUE = "TRUE";
    const CONDITION_MONITORERROR_FALSE = "FALSE"

    const HIGHLIGHT_DEBUG_TEST_LEVEL_FAIL = "FAIL";

    const NODELABEL_NULL = "<<Null>>";
    const NODELABEL_OUTDATEDVER = "<<Outdated version>>";

    const NODELABEL_HIGHRISK = "<<High Risk>>";
    const NODELABEL_CONTRACTTESTFAIL = "<<Contract Testing Fail>>";
    const NODELABEL_MONITORERROR = "<<Monitor Error>>";

    const NODE_SCALE = 1.5;

    let sleuthData = "";
    let sleuthDataLength = 0;
    let totalRequestNum = 0;

    this.data = data;

    let parent = this;
    let collapseData = createCollapseData(data);
    let emptyData = {nodes: [], links: []};
    let graphData = data;

    let canvas = document.getElementById("sdg-canvas");
    let graph = document.getElementById("graph");
    graph.setAttribute("width", canvas.clientWidth);
    graph.setAttribute("height", canvas.clientHeight);
    let body = document.body;
    body.setAttribute("height", canvas.clientHeight);
    let svg = d3.select("svg");
    let graphWidth = +svg.attr("width");
    let graphHeight = +svg.attr("height");


    getSleuthData();

    function resize() {
        graph.setAttribute("width", canvas.clientWidth);
        graph.setAttribute("height", canvas.clientHeight);
        graphWidth = svg.attr("width");
        graphHeight = svg.attr("height");
    }

    window.addEventListener("resize", resize);


    let zoom = d3.zoom()
        .scaleExtent([0.1, 5])
        .on("zoom", zoomed);

    function zoomed() {
        console.log('D3:' + d3.event.transform);
        let transform = d3.event.transform;
        g.attr("transform", transform);
    }

    svg.call(zoom);


    d3.select("#reduce_SVG").on("click", function(d){
        zoom.scaleBy(svg, 0.9); // 執行該方法會觸發zoom事件
        /*        let tran = d3.zoomTransform(svg.node());

                console.log(tran);*/
    });

    d3.select("#increase_SVG").on("click", function(d){
        zoom.scaleBy(svg, 1.1); // 執行該方法會觸發zoom事件
        /*        let tran = d3.zoomTransform(svg.node());

                console.log(tran);*/
    });

    d3.select("#reset_SVG").on("click", function(d){
        svg.call(zoom.transform, d3.zoomIdentity);
    });



    // 畫相依圖
    let simulation = d3.forceSimulation(data.nodes)
        .force("link", d3.forceLink(data.links)
            .id(d => d.id)
            .distance(180)
            .strength(2))
        .force("charge", d3.forceManyBody()
            .strength(-1000)
            .distanceMax(1000))
        .force("x", d3.forceX(graphWidth / 2)
            .strength(0.1))
        .force("y", d3.forceY(graphHeight / 2)
            .strength(0.1))
        .force("collision", d3.forceCollide().radius(90).strength(0.7))
        .velocityDecay(0.9)
        .alphaTarget(0.2)
        .on("tick", ticked);

    let t = d3.transition().duration(600);
    let td = d3.transition().duration(600).delay(500);

    let color = d3.scaleOrdinal(d3.schemeSet2);


    let g = svg.append("g");

    let link = g.append("g").attr("class", "links").selectAll("line");

    let node = g.append("g").attr("class", "nodes").selectAll("path");

    let nodelabel = g.append("g").attr("class", "node-labels").selectAll("g");

    let enterOrExitEvent = true;

    update(graphData);

    function createCollapseData (d) {
        let collapseData = $.extend(true, {}, d);

        collapseData.nodes.filter(node => node.labels.includes(LABEL_SERVICE))
            .forEach(service => {
                collapseData.links.filter(link => (link.source === service.id) && (link.type === REL_OWN))
                    .forEach(own => {
                        collapseData.links.filter(link => link.source === own.target)
                            .forEach(link => link.source = service.id);
                        collapseData.links.filter(link => (link.target === own.target) && (link.type !== REL_OWN))
                            .forEach(link => link.target = service.id);
                    });
            });

        collapseData.nodes = collapseData.nodes.filter(node => !node.labels.includes(LABEL_ENDPOINT));
        collapseData.links = collapseData.links.filter(link => link.type !== REL_OWN);

        return collapseData;
    }

    /*
    function initCollapseData() {
        data.nodes.filter(node => node.labels.includes(LABEL_SERVICE))
            .forEach(service => {
                data.links.filter(link => (link.source === service.id) && (link.type === REL_OWN))
                    .forEach(own => {
                        data.links.filter(link => link.source === own.target)
                            .forEach(link => {
                                link.canBeCollapse = true;
                                let collapseOnlyLink = $.extend(true, {}, link);
                                collapseOnlyLink.source = service.id;
                                collapseOnlyLink.collapseOnly = true;
                                data.links.push(collapseOnlyLink);
                            });
                        data.links.filter(link => (link.target === own.target) && (link.type !== REL_OWN))
                            .forEach(link => {
                                link.canBeCollapse = true;
                                let collapseOnlyLink = $.extend(true, {}, link);
                                collapseOnlyLink.target = service.id;
                                collapseOnlyLink.collapseOnly = true;
                                data.links.push(collapseOnlyLink);
                            });
                    });
            });
    }*/

    this.updateData = function (d) {
        let oldData = $.extend(true, {}, data);

        // REMOVE old nodes
        oldData.nodes.forEach(oldNode => {
            let exist = false;
            d.nodes.some(newNode => {
                if (oldNode.id === newNode.id) {
                    exist = true;
                    return true;
                }
            });
            if (!exist) {
                //console.log("Remove node: ");
                //console.log(oldNode);
                data.nodes.splice(data.nodes.findIndex(node => node.id === oldNode.id), 1);
                enterOrExitEvent = true;
            }
        });

        // UPDATE old nodes
        data.nodes.forEach((oldNode) => {
            let newNode = d.nodes.find(node => node.id === oldNode.id);
            //console.log(JSON.stringify(oldNode.labels.sort()) + "vs" + JSON.stringify(newNode.labels.sort()));
            //console.log(!(JSON.stringify(oldNode.labels.sort()) === JSON.stringify(newNode.labels.sort())));
            if ((JSON.stringify(oldNode.labels.sort()) !== JSON.stringify(newNode.labels.sort()))) {
                //console.log("Labels update: " + newNode.id);
                //console.log(newNode.labels);
                oldNode.labels = newNode.labels;
            }
            if (oldNode.number !== newNode.number) {
                oldNode.number = newNode.number;
            }
        });

        // ADD new nodes
        d.nodes.forEach(newNode => {
            let exist = false;
            oldData.nodes.some(oldNode => {
                if (newNode.id === oldNode.id) {
                    exist = true;
                    return true;
                }
            });
            if (!exist) {
                //console.log("Add node: ");
                //console.log(newNode);
                data.nodes.push(newNode);
                enterOrExitEvent =true;
            }
        });

        // REMOVE old links
        oldData.links.forEach(oldLink => {
            let exist = false;
            d.links.some(newLink => {
                if (oldLink.type === newLink.type && oldLink.source.id === newLink.source && oldLink.target.id === newLink.target) {
                    exist = true;
                    return true;
                }
            });
            if (!exist) {
                //console.log("Remove link:");
                //console.log(oldLink);
                data.links.splice(data.links.findIndex(link => link.type === oldLink.type && link.source.id === oldLink.source.id && link.target.id === oldLink.target.id), 1);
                enterOrExitEvent = true;
            }
        });
        // ADD new links
        d.links.forEach(newLink => {
            let exist = false;
            oldData.links.some(oldLink => {
                if (oldLink.type === newLink.type && oldLink.source.id === newLink.source && oldLink.target.id === newLink.target) {
                    exist = true;
                    return true;
                }
            });
            if (!exist) {
                //console.log("Add link:");
                //console.log(newLink);
                data.links.push(newLink);
                enterOrExitEvent = true;
            }
        });

        updateCollapseData(d);
        update(graphData);
    };

    function updateCollapseData (d) {
        if (collapseData.links.length > 0 && !collapseData.links[0].source.hasOwnProperty("id")) {
            // If collapseData has not been init by D3.js yet.
            collapseData = createCollapseData(d);
        } else {
            let newCollapseData = createCollapseData(d);

            // REMOVE old nodes
            collapseData.nodes = collapseData.nodes.filter(oldNode =>
                newCollapseData.nodes.find(newNode => oldNode.id === newNode.id)
            );

            // UPDATE old nodes
            collapseData.nodes.forEach(oldNode => {
                let newNode = newCollapseData.nodes.find(node => node.id === oldNode.id);
                if (JSON.stringify(oldNode.labels.sort()) !== JSON.stringify(newNode.labels.sort())) {
                    oldNode.labels = newNode.labels;
                }
                if (oldNode.number !== newNode.number) {
                    oldNode.number = newNode.number;
                }
            });

            // ADD new nodes
            newCollapseData.nodes.forEach(newNode => {
                if (!collapseData.nodes.find(oldNode => newNode.id === oldNode.id)) {
                    collapseData.nodes.push(newNode);
                }
            });

            // REMOVE old links
            collapseData.links = collapseData.links.filter(oldLink =>
                newCollapseData.links.find(newLink =>
                    (oldLink.type === newLink.type) &&
                    (oldLink.source.id === newLink.source) &&
                    (oldLink.target.id === newLink.target))
            );

            // ADD new links
            newCollapseData.links.forEach(newLink => {
                if (!collapseData.links.find(oldLink =>
                    (oldLink.type === newLink.type) &&
                    (oldLink.source.id === newLink.source) &&
                    (oldLink.target.id === newLink.target))) {
                    collapseData.links.push(newLink);
                }
            });
        }
    }

    function highlight(d) {
        data.nodes.forEach(node => { node.highlight = false });
        data.links.forEach(link => { link.highlight = false });
        collapseData.nodes.forEach(node => { node.highlight = false });
        collapseData.links.forEach(link => { link.highlight = false });

        d.nodes.forEach(HNode => {
            findNodeById(HNode.id).highlight = true;
            let colNode = collapseData.nodes.find(node => node.id === HNode.id);
            if (colNode) colNode.highlight = true;
        });

        d.links.forEach(HLink => {
            findLinkById(HLink.type + ":" + HLink.source + "-" + HLink.target).highlight = true;
            let colLink = collapseData.links.find(link =>
                link.type === HLink.type &&
                link.source.id === HLink.source &&
                link.target.id === HLink.target);
            if (colLink) colLink.highlight = true;
        });

        update(graphData);
    }

    function highlight_error(d) {
        data.nodes.forEach(node => { node.highlight_error = false });
        data.links.forEach(link => { link.highlight_error = false });
        collapseData.nodes.forEach(node => { node.highlight_error = false });
        collapseData.links.forEach(link => { link.highlight_error = false });

        data.nodes.forEach(node => { node.highlight_error_source = false });
        data.links.forEach(link => { link.highlight_error_source = false });
        collapseData.nodes.forEach(node => { node.highlight_error_source = false });
        collapseData.links.forEach(link => { link.highlight_error_source = false });

        d.nodes.forEach(HNode => {
            findNodeById(HNode.id).highlight_error = true;
            let colNode = collapseData.nodes.find(node => node.id === HNode.id);
            if (colNode) colNode.highlight_error = true;
        });

        d.links.forEach(HLink => {
            findLinkById(HLink.type + ":" + HLink.source + "-" + HLink.target).highlight_error = true;
            let colLink = collapseData.links.find(link =>
                link.type === HLink.type &&
                link.source.id === HLink.source &&
                link.target.id === HLink.target);
            if (colLink) colLink.highlight_error = true;
        });

        d.sourceNodes.forEach(HNode => {
            findNodeById(HNode.id).highlight_error_source = true;
            let colNode = collapseData.nodes.find(node => node.id === HNode.id);
            if (colNode) colNode.highlight_error_source = true;
        });

        d.sourceLinks.forEach(HLink => {
            findLinkById(HLink.type + ":" + HLink.source + "-" + HLink.target).highlight_error_source = true;
            let colLink = collapseData.links.find(link =>
                link.type === HLink.type &&
                link.source.id === HLink.source &&
                link.target.id === HLink.target);
            if (colLink) colLink.highlight_error_source = true;
        });

        update(graphData);
    }

    function clearHighlight() {
        data.nodes.forEach(node => { node.highlight = false });
        data.links.forEach(link => { link.highlight = false });
        collapseData.nodes.forEach(node => { node.highlight = false });
        collapseData.links.forEach(link => { link.highlight = false });

        data.nodes.forEach(node => { node.highlight_error = false });
        data.links.forEach(link => { link.highlight_error = false });
        collapseData.nodes.forEach(node => { node.highlight_error = false });
        collapseData.links.forEach(link => { link.highlight_error = false });

        data.nodes.forEach(node => { node.highlight_error_source = false });
        data.links.forEach(link => { link.highlight_error_source = false });
        collapseData.nodes.forEach(node => { node.highlight_error_source = false });
        collapseData.links.forEach(link => { link.highlight_error_source = false });
        update(graphData);
    }

    function findNodeById(id) {
        let result;
        data.nodes.some(node => {
            if (node.id === id) {
                result = node;
                return true;
            }
        });
        return result;
    }

    function findNodeById_returnResult(id) {
        let result;
        result = data.nodes.find(node => node.id === id);
        return result;
    }

    function findParentById(id) {
        let result;
        let link2;

        link2 = data.links.find(link => (link.target.id === id) && (link.type === REL_OWN));
        result = data.nodes.find(node => node.id === link2.source.id);
        return result;
    }

    function findLinkById(id) {
        let result;
        data.links.some(link => {
            if (link.type + ":" + link.source.id + "-" + link.target.id === id) {
                result = link;
                return true;
            }
        });
        return result;
    }

    function findLinkById_returnResult(id) {
        let result;
        result = data.links.find(link => (link.type + ":" + link.source.id + "-" + link.target.id) === id);

        return result;
    }

    function getSleuthData(){
        return fetch("/web-page/sleuth/getAllServiceAndPathWithHTTP_REQUEST")
            .then(response => response.json())
            .then(responseJSON => {
                //console.log("function getSleuthData()_responseJSON:::\n" + JSON.stringify(responseJSON));
                sleuthData = responseJSON;
                sleuthDataLength = responseJSON.length;

                for (let i = 0; i < sleuthDataLength; i++) {
                    //console.log("sleuthData:：:：" + JSON.stringify(responseJSON[i]));
                    totalRequestNum += parseInt(responseJSON[i].num);
                }
            });
    }

    async function update(data) {
        //console.log(data);
        console.log("update");

        await getSleuthData();

        /*
        let nodes = collapse ? data.nodes.filter(node => !node.labels.includes(LABEL_ENDPOINT)) : data.nodes;
        let links = collapse ? data.links.filter(link =>
            !link.canBeCollapse || link.collapseOnly) : data.links.filter(link => !link.collapseOnly);
        console.log(links);
        */

        simulation.nodes(data.nodes);
        simulation.force("link").links(data.links);

        // JOIN data and event listeners with old links
        link = link.data(data.links, d => { return d.type + ":" + d.source.id + "-" + d.target.id });

        // EXIT old links
        // transition(t)
        link.exit().transition()
            .attr("stroke-width", 0)
            .remove();

        // UPDATE old links
        link.transition(t);

        link.filter(d => !d.highlight)
            .classed("highlight", false)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l)"
                    } else {
                        return "url(#arrow-m)";
                    }
                } else if (d.type === REL_HTTPREQUEST) {
                    return "url(#arrow-request)";
                }else if (d.type === REL_NEWERPATCHVERSION) {
                    return "url(#arrow-l-warning)";
                }
            });
        link.filter(d => d.highlight)
            .classed("highlight", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight)"
                    } else {
                        return "url(#arrow-m-highlight)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });

        link.filter(d => !d.highlight_error)
            .classed("highlight_error", false);

        link.filter(d => d.highlight_error)
            .classed("highlight_error", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight_error)"
                    } else {
                        return "url(#arrow-m-highlight_error)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });

        link.filter(d => !d.highlight_error_source)
            .classed("highlight_error_source", false);

        link.filter(d => d.highlight_error_source)
            .classed("highlight_error_source", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight_error_source)"
                    } else {
                        return "url(#arrow-m-highlight_error_source)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });


        link.filter(d => !(d.type === REL_NEWERPATCHVERSION))
            .classed("warning", false)
            .classed("dash", false);
        link.filter(d => d.type === REL_NEWERPATCHVERSION)
            .classed("warning", true)
            .classed("dash", true);

        // ENTER new links
        let linkEnter = link.enter().append("g");

        linkEnter.append("line")
            .attr("stroke-width", d => {

                // 根據端點請求數量更改線的粗細
                if (d.type === REL_HTTPREQUEST && sleuthDataLength !== 0) {
                    for(let i = 0; i < sleuthDataLength; i++){
                        let targetNode = data.links.find(targetNode => targetNode.target === d.target);
                        //console.log("targetNode: " + JSON.stringify(targetNode));
                        //console.log("sleuthData[i].targetServiceVersion: " + sleuthData[i].targetServiceVersion);
                        //console.log("targetNode.version: " + targetNode.source.version);
                        if(d.source.path === sleuthData[i].path &&
                            d.source.appName === sleuthData[i].appName &&
                            sleuthData[i].targetServiceVersion === targetNode.source.version &&
                            sleuthData[i].targetAppName === targetNode.source.appName) {

                            if(parseInt(sleuthData[i].num) === 0)
                                return 3;
                            else
                                return 3 + 15 * ( parseInt(sleuthData[i].num)/parseInt(totalRequestNum) );
                        }
                    }
                }else{
                    return 3;
                }
            })
            .attr("marker-end", d => {
                if (d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l)"
                    } else {
                        return "url(#arrow-m)";
                    }
                } else if (d.type === REL_HTTPREQUEST) {
                    if (sleuthDataLength !== 0) {
                        for(let i = 0; i < sleuthDataLength; i++){
                            let targetNode = data.links.find(targetNode => targetNode.target === d.target);
                            //console.log("targetNode: " + JSON.stringify(targetNode));
                            //console.log("sleuthData[i].targetServiceVersion: " + sleuthData[i].targetServiceVersion);
                            //console.log("targetNode.version: " + targetNode.source.version);
                            if(d.source.path === sleuthData[i].path &&
                                d.source.appName === sleuthData[i].appName &&
                                sleuthData[i].targetServiceVersion === targetNode.source.version &&
                                sleuthData[i].targetAppName === targetNode.source.appName) {

                                if(parseInt(sleuthData[i].num) === 0) {
                                    return "url(#arrow-request-m)";
                                }else {
                                    return "url(#arrow-request)";
                                }
                            }
                        }
                    }else{
                        return "url(#arrow-request-m)";
                    }
                }else if (d.type === REL_NEWERPATCHVERSION) {
                    return "url(#arrow-l-warning)";
                }
            })
            .attr("class", d => {
                if (d.type === REL_HTTPREQUEST) {
                    return "request";
                }
            });


        /*---------------------------*/
        linkEnter.filter(d => !d.highlight)
            .classed("highlight", false)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l)"
                    } else {
                        return "url(#arrow-m)";
                    }
                } else if (d.type === REL_HTTPREQUEST) {
                    return "url(#arrow-request)";
                }else if (d.type === REL_NEWERPATCHVERSION) {
                    return "url(#arrow-l-warning)";
                }
            });
        linkEnter.filter(d => d.highlight)
            .classed("highlight", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight)"
                    } else {
                        return "url(#arrow-m-highlight)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });

        linkEnter.filter(d => !d.highlight_error)
            .classed("highlight_error", false);

        linkEnter.filter(d => d.highlight_error)
            .classed("highlight_error", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight_error)"
                    } else {
                        return "url(#arrow-m-highlight_error)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });

        linkEnter.filter(d => !d.highlight_error_source)
            .classed("highlight_error_source", false);

        linkEnter.filter(d => d.highlight_error_source)
            .classed("highlight_error_source", true)
            .selectAll("line")
            .attr("marker-end", d => {
                if (d.type === REL_HTTPREQUEST || d.type === REL_AMQPPUBLISH || d.type === REL_AMQPSUBSCRIBE) {
                    if (d.target.labels.includes(LABEL_SERVICE) || d.target.labels.includes(LABEL_QUEUE)) {
                        return"url(#arrow-l-highlight_error_source)"
                    } else {
                        return "url(#arrow-m-highlight_error_source)";
                    }
                } else if (d.type === REL_NEWERPATCHVERSION) {
                    return"url(#arrow-l-warning)"
                }
            });


        linkEnter.filter(d => d.type === REL_NEWERPATCHVERSION)
            .classed("warning", true)
            .classed("dash", true);

        linkEnter.filter(d => { return d.type !== REL_OWN })
            .append("text")
            .attr("fill-opacity", 0)
            .text(d => {
                switch (d.type) {
                    case REL_HTTPREQUEST:
                        return REL_TEXT_REL_HTTPREQUEST;
                    case REL_AMQPSUBSCRIBE:
                        return REL_TEXT_AMQPSUBSCRIBE;
                    case REL_AMQPPUBLISH:
                        return REL_TEXT_AMQPPUBLISH;
                    case REL_NEWERPATCHVERSION:
                        return REL_TEXT_NEWERPATCHVERSION;
                }
            })
            .style("pointer-events", "none")
            .transition(td).attr("fill-opacity", 1);

        link = linkEnter.merge(link);

        // JOIN data and event listeners with old nodes
        node = node.data(data.nodes, function(d) { return d.id;});

        // EXIT old nodes
        // transition(t)
        node.exit().transition()
            .attr("fill-opacity", 0)
            .remove();

        // UPDATE old nodes
        node.transition(t);

        // Highlight
        node.filter(d =>
            !d.labels.includes(LABEL_OUTDATEDVERSION) ||
            !d.labels.includes(LABEL_HEAVY_STRONG_UPPER_DEPENDENCY) ||
            !d.labels.includes(LABEL_HEAVY_STRONG_LOWER_DEPENDENCY) ||
            !d.labels.includes(LABEL_HEAVY_WEAK_UPPER_DEPENDENCY) ||
            !d.labels.includes(LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)
        ).classed(HIGHLIGHT_LEVEL_WARNING, false);
        node.filter(d => !d.warning).classed(HIGHLIGHT_LEVEL_WARNING, false);
        node.filter(d =>
            d.labels.includes(LABEL_OUTDATEDVERSION) ||
            d.labels.includes(LABEL_HEAVY_STRONG_UPPER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_STRONG_LOWER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_WEAK_UPPER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)
        ).classed(HIGHLIGHT_LEVEL_WARNING, true);
        node.filter(d => d.warning).classed(HIGHLIGHT_LEVEL_WARNING, true);

        node.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_WARNING)
            .classed(HIGHLIGHT_CONTRACTTESTING_WARNING,true);
        node.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_PASS)
            .classed(HIGHLIGHT_CONTRACTTESTING_WARNING,false);

        node.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_TRUE)
            .classed(HIGHLIGHT_HIGHRISK_TRUE,true);
        node.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_FALSE)
            .classed(HIGHLIGHT_HIGHRISK_TRUE,false);

        node.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_TRUE)
            .classed(HIGHLIGHT_MONITORERROR_TRUE,true);
        node.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_FALSE)
            .classed(HIGHLIGHT_MONITORERROR_TRUE,false);




        node.filter(d => !d.labels.includes(LABEL_NULLSERVICE) && !d.labels.includes(LABEL_NULLENDPOINT))
            .classed(HIGHLIGHT_LEVEL_ERROR, false);
        node.filter(d => !d.error).classed(HIGHLIGHT_LEVEL_ERROR, false);
        node.filter(d => d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT))
            .classed(HIGHLIGHT_LEVEL_ERROR, true);
        node.filter(d => d.error).classed(HIGHLIGHT_LEVEL_ERROR, true);

        node.filter(d => !d.highlight).classed(HIGHLIGHT_LEVEL_NORMAL, false);
        node.filter(d => d.highlight).classed(HIGHLIGHT_LEVEL_NORMAL, true);

        node.filter(d => !d.highlight_error).classed(HIGHLIGHT_MONITORERROR, false);
        node.filter(d => d.highlight_error).classed(HIGHLIGHT_MONITORERROR, true);

        node.filter(d => !d.highlight_error_source).classed(HIGHLIGHT_MONITORERROR_SOURCE, false);
        node.filter(d => d.highlight_error_source).classed(HIGHLIGHT_MONITORERROR_SOURCE, true);

        node.attr("fill", d => {
            if (d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT)) {
                return COLOR_NULL;
            } else if (d.labels.includes(LABEL_QUEUE)) {
                return COLOR_QUEUE;
            } else {
                return color(d.appName);
            }
        });

        // ENTER new nodes
        let nodeEnter = node.enter().append("path")
            .attr("class", "node")
            .attr("fill", d => {
                if (d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT)) {
                    return COLOR_NULL;
                } else if (d.labels.includes(LABEL_QUEUE)) {
                    return COLOR_QUEUE;
                } else {
                    return color(d.appName);
                }
            })
            .attr("stroke-opacity", 1)
            .attr("fill-opacity", 1);

        /*        nodeEnter.transition()
                    .attr("stroke-opacity", 1)
                    .attr("fill-opacity", 1);*/


        // Highlight
        nodeEnter.filter(d =>
            d.labels.includes(LABEL_OUTDATEDVERSION) ||
            d.labels.includes(LABEL_HEAVY_STRONG_UPPER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_STRONG_LOWER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_WEAK_UPPER_DEPENDENCY) ||
            d.labels.includes(LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)
        ).classed(HIGHLIGHT_LEVEL_WARNING, true);

        nodeEnter.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_WARNING)
            .classed(HIGHLIGHT_CONTRACTTESTING_WARNING,true);
        nodeEnter.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_PASS)
            .classed(HIGHLIGHT_CONTRACTTESTING_WARNING,false);

        nodeEnter.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_TRUE)
            .classed(HIGHLIGHT_HIGHRISK_TRUE,true);
        nodeEnter.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_FALSE)
            .classed(HIGHLIGHT_HIGHRISK_TRUE,false);

        nodeEnter.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_TRUE)
            .classed(HIGHLIGHT_MONITORERROR_TRUE,true);
        nodeEnter.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_FALSE)
            .classed(HIGHLIGHT_MONITORERROR_TRUE,false);


        nodeEnter.filter(d => d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT))
            .classed(HIGHLIGHT_LEVEL_ERROR, true);

        nodeEnter.filter(d => d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_ENDPOINT))
            .attr("d", d3.symbol()
                .size(d => {
                    if(d.labels.includes(LABEL_SERVICE)) {
                        return SIZE_SERVIVE;
                    } else if(d.labels.includes(LABEL_ENDPOINT)) {
                        return SIZE_ENDPOINT;
                    }
                })
                .type((d, i) => {
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return SYMBOL_SERVIVE;
                    } else if(d.labels.includes(LABEL_ENDPOINT)){
                        return SYMBOL_ENDPOINT;
                    }
                })
            );

        nodeEnter.filter(d => d.labels.includes(LABEL_QUEUE))
            .attr("d", d3.symbol()
                .size(d => {
                    if(d.labels.includes(LABEL_QUEUE)) {
                        return SIZE_QUEUE;
                    }
                })
                .type((d, i) => {
                    if (d.labels.includes(LABEL_QUEUE)) {
                        return SYMBOL_QUEUE;
                    }
                }));

        nodeEnter.append("title")
            .text(d => {
                if (d.labels.includes(LABEL_SERVICE)) {
                    return LABEL_SERVICE;
                } else if (d.labels.includes(LABEL_ENDPOINT)) {
                    return LABEL_ENDPOINT;
                } else if (d.labels.includes(LABEL_QUEUE)) {
                    return LABEL_QUEUE;
                }
            });

        node = nodeEnter.merge(node);

        node.on("mouseover", mouseover)
            .on("mouseout", mouseout)
            .on("click", clicked)
            .call(d3.drag()
                .on("start", dragstarted)
                .on("drag", dragged)
                .on("end", dragended));

        // JOIN data and event listeners with old nodelabels
        nodelabel = nodelabel.data(data.nodes, function(d) { return d.id;});

        // EXIT old nodelabels
        nodelabel.exit().remove();

        // UPDATE old nodelabels
        nodelabel.selectAll("rect").remove();
        nodelabel.selectAll("text").remove();

        nodelabel.filter(d =>  d.labels.includes(LABEL_SERVICE))
            .append("text")
            .attr("class", "number-of-instances")
            .attr("fill-opacity", 0.3)
            .attr("alignment-baseline", "central")
            .style("font-size", 28)
            .style("fill", "#000000")
            .text(d => d.riskValue.toFixed(2)); //***Risk********************************************

        nodelabel.append("rect")
            .attr("class", "tag")
            .attr("fill", "#dddddd")
            .attr("fill-opacity", 0.5)
            .attr("rx", 8)
            .attr("ry", 8);

        nodelabel.append("text")
            .attr("class", "tag")
            .attr("dx", 0)
            .attr("dy", d => {
                if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_QUEUE)) {
                    return 53;
                } else if (d.labels.includes(LABEL_ENDPOINT)) {
                    return 39;
                }
            })
            .attr("fill-opacity", 1)
            .text(d => {
                if (d.labels.includes(LABEL_SERVICE)) {
                    return d.appName + ":" + d.version + "(" + d.number + ")";
                } else if (d.labels.includes(LABEL_ENDPOINT)) {
                    return "[" + d.method + "] " + d.path;
                } else if (d.labels.includes(LABEL_QUEUE)) {
                    return d.queueName;
                }
            });

        nodelabel.selectAll("rect.tag")
            .attr("width", function() {
                return (d3.select(this.parentNode).select("text.tag").node().getBBox().width + 8);
            })
            .attr("height", "16px")
            .attr("x", function() {
                return (d3.select(this.parentNode).select("text.tag").node().getBBox().width + 8) / -2;
            }).attr("y", d => {
            if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_QUEUE)) {
                return 40;
            } else if (d.labels.includes(LABEL_ENDPOINT)) {
                return 28;
            }
        });


        let contractTestingNodelabel = nodelabel.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_WARNING);
        updateContractTestFailNodeLabel(contractTestingNodelabel, NODELABEL_CONTRACTTESTFAIL);

        let highRiskNodelabel = nodelabel.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_TRUE);
        updateHighRiskNodeLabel(highRiskNodelabel, NODELABEL_HIGHRISK);

        let monitorErrorNodelabel = nodelabel.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_TRUE);
        updateMonitorErrorNodeLabel(monitorErrorNodelabel, NODELABEL_MONITORERROR);

        let oldNullNodelabel = nodelabel.filter(d =>  d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT));
        updateExceptionNodeLabel(oldNullNodelabel, NODELABEL_NULL);

        let oldOutDateVerNodeLabel = nodelabel.filter(d => d.labels.includes(LABEL_OUTDATEDVERSION));
        updateExceptionNodeLabel(oldOutDateVerNodeLabel, NODELABEL_OUTDATEDVER);

        function updateExceptionNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag null-tag")
                .attr("fill", "#00000000")
                .attr("fill-opacity", 0.5)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag null-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 1)
                .style("fill", "#ce0000")
                .text(text);

            nodeLabel.selectAll("rect.null-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.null-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.null-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function updateContractTestFailNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag contractTestFail-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0.5)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag contractTestFail-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 1)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.contractTestFail-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.contractTestFail-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.contractTestFail-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function updateHighRiskNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag highRisk-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0.5)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag highRisk-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 1)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.highRisk-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.highRisk-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.highRisk-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function updateMonitorErrorNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag monitorError-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0.5)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag monitorError-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 1)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.monitorError-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.monitorError-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.monitorError-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        // ENTER new nodelabels
        let nodelabelEnter = nodelabel.enter().append("g");

        let serviceNodesNum = nodelabelEnter.filter(d => {
            return d.labels.includes(LABEL_SERVICE);
        });

        serviceNodesNum.append("text")
            .attr("class", "number-of-instances")
            .attr("fill-opacity", 0)
            .attr("alignment-baseline", "central")
            .style("font-size", 28)
            .style("fill", "#000000")
            .text(d => {
                if (d.labels.includes(LABEL_SERVICE)) {
                    //return d.number; //*********************修改Risk的地方*************************************************
                    return d.riskValue.toFixed(2);
                }
            });

        nodelabelEnter.append("rect")
            .attr("class", "tag")
            .attr("fill", "#dddddd")
            .attr("fill-opacity", 0)
            .attr("rx", 8)
            .attr("ry", 8);

        nodelabelEnter.append("text")
            .attr("class", "tag")
            .attr("dx", 0)
            .attr("dy", d => {
                if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_QUEUE)) {
                    return 53;
                } else if (d.labels.includes(LABEL_ENDPOINT)) {
                    return 39;
                }
            })
            .attr("fill-opacity", 0)
            .text(d => {
                if (d.labels.includes(LABEL_SERVICE)) {
                    return d.appName + ":" + d.version + "(" + d.number + ")";
                } else if (d.labels.includes(LABEL_ENDPOINT)) {
                    return "[" + d.method + "] " + d.path;
                } else if (d.labels.includes(LABEL_QUEUE)) {
                    return d.queueName;
                }
            });

        nodelabelEnter.selectAll("rect.tag")
            .attr("width", function() {
                return (d3.select(this.parentNode).select("text.tag").node().getBBox().width + 8);
            })
            .attr("height", "16px")
            .attr("x", function() {
                return (d3.select(this.parentNode).select("text.tag").node().getBBox().width + 8) / -2;
            }).attr("y", d => {
            if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_QUEUE)) {
                return 40;
            } else if (d.labels.includes(LABEL_ENDPOINT)) {
                return 28;
            }
        });

        let contractTestingNodelabelEnter = nodelabelEnter.filter(d => d.contractTestingCondition === CONDITION_CONTRACTTEST_WARNING);
        addContractTestFailNodeLabel(contractTestingNodelabelEnter, NODELABEL_CONTRACTTESTFAIL);

        let highRiskNodelabelEnter = nodelabelEnter.filter(d => d.highRiskCondition === CONDITION_HIGHRISK_TRUE);
        addHighRiskNodeLabel(highRiskNodelabelEnter, NODELABEL_HIGHRISK);

        let monitorErrorNodelabelEnter = nodelabelEnter.filter(d => d.monitorErrorCondition === CONDITION_MONITORERROR_TRUE);
        addMonitorErrorNodeLabel(monitorErrorNodelabelEnter, NODELABEL_MONITORERROR);

        let nullNodelabel = nodelabelEnter.filter(d => d.labels.includes(LABEL_NULLSERVICE) || d.labels.includes(LABEL_NULLENDPOINT));
        addExceptionNodeLabel(nullNodelabel, NODELABEL_NULL);

        let outDateVerNodeLabel = nodelabelEnter.filter(d => d.labels.includes(LABEL_OUTDATEDVERSION));
        addExceptionNodeLabel(outDateVerNodeLabel, NODELABEL_OUTDATEDVER);

        function addExceptionNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag null-tag")
                .attr("fill", "#dddddd")
                .attr("fill-opacity", 0)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag null-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 0)
                .style("fill", "#ce0000")
                .text(text);

            nodeLabel.selectAll("rect.null-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.null-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.null-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function addContractTestFailNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag contractTestFail-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag contractTestFail-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 0)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.contractTestFail-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.contractTestFail-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.contractTestFail-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function addHighRiskNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag highRisk-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag highRisk-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 0)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.highRisk-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.highRisk-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.highRisk-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        function addMonitorErrorNodeLabel (nodeLabel, text) {
            nodeLabel.append("rect")
                .attr("class", "tag monitorError-tag")
                .attr("fill", "#ffc107")
                .attr("fill-opacity", 0)
                .attr("rx", 8)
                .attr("ry", 8);

            nodeLabel.append("text")
                .attr("class", "tag monitorError-tag")
                .attr("dx", 0)
                .attr("dy", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position = texts.length - 1;
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 53 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 39 + position * 20;
                    }
                })
                .attr("fill-opacity", 0)
                .style("fill", "#212529")
                .text(text);

            nodeLabel.selectAll("rect.monitorError-tag")
                .attr("width", function() {
                    let text = d3.select(this.parentNode).select("text.monitorError-tag").node();
                    return (text.getBBox().width + 8);
                })
                .attr("height", "16px")
                .attr("x", function() {
                    let text = d3.select(this.parentNode).select("text.monitorError-tag").node();
                    return (text.getBBox().width + 8) / -2;
                })
                .attr("y", function (d) {
                    let texts = $(this.parentNode).find("text.tag");
                    let position;
                    for (position = 0; position < texts.length; position++) {
                        if (texts[position].textContent === text) {
                            break;
                        }
                    }
                    if (d.labels.includes(LABEL_SERVICE)) {
                        return 40 + position * 20;
                    } else if (d.labels.includes(LABEL_ENDPOINT)) {
                        return 27 + position * 20;
                    }
                });
        }

        nodelabelEnter.style("pointer-events", "none");

        nodelabelEnter.selectAll("text.number-of-instances")
            .transition(td)
            .attr("fill-opacity", 0.3);

        nodelabelEnter.selectAll("rect.tag")
            .transition(td)
            .attr("fill-opacity", 0.5);

        nodelabelEnter.selectAll("text.tag")
            .transition(td)
            .attr("fill-opacity", 1);

        nodelabel = nodelabelEnter.merge(nodelabel);

        if (enterOrExitEvent) {
            simulation.alpha(1);
        }
        simulation.alphaTarget(0.3).restart();
        enterOrExitEvent = false;

    }

    // 定義ticked()，用來當tick發現數據改變時，要做的動作
    function ticked() {
        link.selectAll("line")
            .attr("x1", d => { return d.source.x; })
            .attr("y1", d => { return d.source.y; })
            .attr("x2", d => { return d.target.x; })
            .attr("y2", d => { return d.target.y; });

        link.selectAll("text")
            .attr("x", d => { return (d.source.x + d.target.x) / 2})
            .attr("y", d => {
                let y = (d.source.y + d.target.y) / 2;
                if (d.target.x > d.source.x) {
                    return y - 3;
                } else {
                    return y + 8;
                }
            })
            .attr("transform", d => {
                let x = (d.source.x + d.target.x) / 2;
                let y = (d.source.y + d.target.y) / 2;
                let deg;
                if (d.target.x > d.source.x) {
                    deg = culDegrees(d.source.x, d.source.y, d.target.x, d.target.y);
                } else {
                    deg = culDegrees(d.target.x, d.target.y, d.source.x, d.source.y);
                }
                return "rotate(" + deg + " " + x + " " + y +  ")";
            });

        node.attr("transform", d => {
            return "translate(" + d.x + "," + d.y + ")";
        });

        nodelabel.attr("transform", d => { return "translate(" + d.x + "," + d.y + ")"; });

    }

    function culDegrees(x1, y1, x2, y2) {
        return Math.atan2(y2 - y1, x2 - x1)*180/Math.PI;
    }

    function clicked(d) {
        /*
        let scale;
        let translate;
        if (graphWidth > 960) {
            scale = 1.5;
            translate = [graphWidth * 0.33 - scale * d.x, graphHeight / 2 - scale * d.y];
        } else {
            scale = 1;
            translate = [graphWidth / 2 - scale * d.x, graphHeight * 0.3 - scale * d.y];
        }
        */
        let scale = 1;
        let translate = [graphWidth / 2 - scale * d.x, graphHeight / 2 - scale * d.y];
        let transform = d3.zoomIdentity
            .translate(translate[0], translate[1])
            .scale(scale);
        svg.transition().duration(600).call(zoom.transform, transform);

        openNodeCard(d, d3.event.active);
        parent.selectedNode = d;
        $(parent).trigger('selectNode', d);
    }

    // 定義拖拉的動作，因為在拖拉的過程中，會中斷模擬器，所以利用restart來重啟
    function dragstarted(d) {
        if (!d3.event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(d) {
        d.fx = d3.event.x;
        d.fy = d3.event.y;
    }

    function dragended(d) {
        if (!d3.event.active) simulation.alphaTarget(0);
        //d.fx = null;
        //d.fy = null;
    }

    function mouseover(d, i) {
        if (!d3.event.active) simulation.alphaTarget(0.3).restart();
        d.fx = null;
        d.fy = null;
        if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_ENDPOINT)) {
            d3.select(this).transition().duration(100)
                .attr("d", d3.symbol()
                    .size(d => {
                        if(d.labels.includes(LABEL_SERVICE)) {
                            return SIZE_SERVIVE * NODE_SCALE;
                        } else if(d.labels.includes(LABEL_ENDPOINT)) {
                            return SIZE_ENDPOINT * NODE_SCALE;
                        }
                    })
                    .type((d, i) => {
                        if (d.labels.includes(LABEL_SERVICE)) {
                            return SYMBOL_SERVIVE;
                        } else if(d.labels.includes(LABEL_ENDPOINT)){
                            return SYMBOL_ENDPOINT;
                        }
                    })
                )
                .attr("fill-opacity", 0.8);
        } else if (d.labels.includes(LABEL_QUEUE)) {
            d3.select(this).transition().duration(100)
                .attr("d", d3.symbol()
                    .size(d => {
                        if(d.labels.includes(LABEL_QUEUE)) {
                            return SIZE_QUEUE * NODE_SCALE;
                        }
                    })
                    .type((d, i) => {
                        if (d.labels.includes(LABEL_QUEUE)) {
                            return SYMBOL_QUEUE;
                        }
                    }))
                .attr("fill-opacity", 0.8);
        }

    }

    function mouseout(d, i) {
        if (d.labels.includes(LABEL_SERVICE) || d.labels.includes(LABEL_ENDPOINT)) {
            d3.select(this).transition().duration(100)
                .attr("d", d3.symbol()
                    .size(d => {
                        if(d.labels.includes(LABEL_SERVICE)) {
                            return SIZE_SERVIVE;
                        } else if(d.labels.includes(LABEL_ENDPOINT)) {
                            return SIZE_ENDPOINT;
                        }
                    })
                    .type((d, i) => {
                        if (d.labels.includes(LABEL_SERVICE)) {
                            return SYMBOL_SERVIVE;
                        } else if(d.labels.includes(LABEL_ENDPOINT)){
                            return SYMBOL_ENDPOINT;
                        }
                    })
                )
                .attr("fill-opacity", 1);
        } else if (d.labels.includes(LABEL_QUEUE)) {
            d3.select(this).transition().duration(100)
                .attr("d", d3.symbol()
                    .size(d => {
                        if(d.labels.includes(LABEL_QUEUE)) {
                            return SIZE_QUEUE;
                        }
                    })
                    .type((d, i) => {
                        if (d.labels.includes(LABEL_QUEUE)) {
                            return SYMBOL_QUEUE;
                        }
                    }))
                .attr("fill-opacity", 1);
        }
    }

    let cardDiv = $("#card-div");
    let card = $("#node-card");
    let cardHeader = card.find(".card-header").first();
    let cardClose = cardHeader.find(".close").first();
    let cardHeaderTitle = cardHeader.find(".card-title").first();

    let cardInfoTab = $("#information-tab");
    let cardGraphTab = $("#graph-tab");
    let cardMonitorTab = $("#monitor-tab");
    let cardContractTab = $("#contract-tab");
    let cardAlertTab = $("#alert-tab");

    let extraMessage = $('#extraMessage');
    let messageJson = $('#message-json');

    let nodeInfoBody = $("#node-infomation .card-body").first();
    //let nodeInfoTitle = nodeInfoBody.find(".card-title").first();

    let nodeGraphBody = $("#node-graph .card-body").first();
    let graphList = $("#graph-list");
    let graphProvider = $("#graph-providers");
    let graphConsumers = $("#graph-consumers");
    let graphUpperDependencyStrong = $("#graph-upper-dependency-strong");
    let graphUpperDependencyWeak = $("#graph-upper-dependency-weak");
    let graphLowerDependencyStrong = $("#graph-lower-dependency-strong");
    let graphLowerDependencyWeak = $("#graph-lower-dependency-weak");

    let nodeMonitorBody = $("#node-monitor .card-body").first();
    let nodeMonitorTitle = nodeMonitorBody.find(".card-title").first();
    let healthJson = $("#health-json");
    let metricsActuratorJson = $("#metrics-actuator-json");
    let metricsElasticsearchJson = $("#metrics-elasticsearch-json");

    let contractGroup = $('#graph-contractList');
    let contractMissingGroup = $('#graph-contractMissingList');
    let serviceCondition = $('#serviceCondition');

    let monitorErrorGroup = $('#graph-MonitorErrorList');

    let monitorErrorMessage = $('#monitorErrorMessage');
    let monitorErrorMessageJson = $('#monitorErrorMessage-json');

    let monitorError_feedbackContract = $('#monitorError_feedbackContract');


    let nodeSettingforms = $("#node-setting-form");

    let failureStatusRateInput = nodeSettingforms.find("#failure-status-rate");
    let failureErrorCountInput = nodeSettingforms.find("#failure-error-count");
    let enableRestFailureAlertInput = nodeSettingforms.find("#enable-rest-failure-alert");
    let enableLogFailureAlertInput = nodeSettingforms.find("#enable-log-failure-alert");

    let thresholdSPCHighDurationRateInput = nodeSettingforms.find("#threshold-spc-high-duration-rate");
    let enableSPCHighDurationRateAlertInput = nodeSettingforms.find("#enable-spc-high-duration-rate-alert");

    let thresholdAverageDurationInput = nodeSettingforms.find("#threshold-average-duration");
    let enableRestAverageDurationAlertInput = nodeSettingforms.find("#enable-rest-average-duration-alert");
    let enableLogAverageDurationAlertInput = nodeSettingforms.find("#enable-log-average-duration-alert");

    let strongUpperDependencyCountInput = nodeSettingforms.find("#strong-upper-dependency-count");
    let strongLowerDependencyCountInput = nodeSettingforms.find("#strong-lower-dependency-count");
    let enableStrongDependencyAlertInput = nodeSettingforms.find("#enable-strong-dependency-alert");
    let weakUpperDependencyCountInput = nodeSettingforms.find("#weak-upper-dependency-count");
    let weakLowerDependencyCountInput = nodeSettingforms.find("#weak-lower-dependency-count");
    let enableWeakDependencyAlertInput = nodeSettingforms.find("#enable-weak-dependency-alert");

    let riskValueAlertInput = nodeSettingforms.find("#risk-value-alert");
    let enableRiskValueAlertInput = nodeSettingforms.find("#enable-risk-value-alert");

    $("#failure-status-rate").on("input", function () {
        $("#failure-status-rate-text").val(this.value + "%");
    }).trigger("change");

    $("#threshold-spc-high-duration-rate").on("input", function () {
        $("#threshold-spc-high-duration-rate-text").val(this.value + "%");
    }).trigger("change");

    $("#risk-value-alert").on("input", function () {
        $("#risk-value-alert-text").val(this.value * 1.0);
    }).trigger("change");

    let stickNode = null;
    let stickEvent = null;

    function openNodeCard(d, event) {
        cardInfoTab.removeClass("show");
        cardGraphTab.removeClass("show");
        cardMonitorTab.removeClass("show");
        cardContractTab.removeClass("show");
        cardAlertTab.removeClass("show");


        if (d.labels.includes(LABEL_SERVICE) && !d.labels.includes(LABEL_NULLSERVICE)) {
            cardInfoTab.addClass("show");
            cardGraphTab.addClass("show");
            cardMonitorTab.addClass("show");
            cardContractTab.addClass("show");
            cardAlertTab.addClass("show");
            if (!(cardGraphTab.hasClass("active") ||
                cardMonitorTab.hasClass("active") ||
                cardAlertTab.hasClass("active")   ||
                cardContractTab.hasClass("active"))) {
                cardInfoTab.tab('show');
            }
        } else {
            cardGraphTab.addClass("show").tab('show');
        }

        // init
        clearHighlight();
        extraMessage.removeClass("show");
        monitorErrorMessage.removeClass("show");
        monitorError_feedbackContract.removeClass("show");
        cardHeaderTitle.empty();

        nodeInfoBody.empty();

        contractGroup.empty();
        monitorErrorGroup.empty();


        graphList.find(".active").removeClass("active");
        graphProvider.unbind();
        graphConsumers.unbind();
        graphUpperDependencyStrong.unbind();
        graphLowerDependencyStrong.unbind();
        graphUpperDependencyWeak.unbind();
        graphLowerDependencyWeak.unbind();

        healthJson.empty();
        metricsActuratorJson.empty();
        metricsElasticsearchJson.empty();

        nodeSettingforms.unbind();
        nodeSettingforms.removeClass("was-validated");

        // Release stick node.
        if (stickNode != null) {
            if (!stickEvent) simulation.alphaTarget(0.3).restart();
            stickNode.fx = null;
            stickNode.fy = null;
        }

        // Remember currently node as stick node.
        stickNode = d;
        stickEvent = event;

        // Close button
        cardClose.on("click", function() {
            clearHighlight();
            cardDiv.removeClass("show");
            extraMessage.removeClass("show");
            monitorErrorMessage.removeClass("show")
            monitorError_feedbackContract.removeClass("show");

            // Release stick node.
            if (!event) simulation.alphaTarget(0.3).restart();
            d.fx = null;
            d.fy = null;

            stickNode = null;
            stickEvent = null;

            parent.selectedNode = null;
        });

        // Card header
        if (d.labels.includes(LABEL_ENDPOINT)) {

            cardHeaderTitle.append("<span class=\"badge badge-pill\">" + d.method.toUpperCase() + "</span>");
            if (d.method === "get") {
                cardHeaderTitle.find(".badge").addClass("badge-primary");
            } else if (d.method === "post") {
                cardHeaderTitle.find(".badge").addClass("badge-success");
            } else if (d.method === "put") {
                cardHeaderTitle.find(".badge").addClass("badge-warning");
            }else if (d.method === "delete") {
                cardHeaderTitle.find(".badge").addClass("badge-danger");
            }
            cardHeaderTitle.append(" " + d.path);
        } else if (d.labels.includes(LABEL_SERVICE)) {
            /************************************************************************/
            // <a href="#" class="badge badge-success">Success</a>
            //<span class="badge badge-pill badge-success">Success</span>
            cardHeaderTitle.append("<a id=\"serviceCondition\" href=\"#\" onclick=\"document.getElementById('contract-tab').click()\" class=\"badge badge-pill badge-success\">" + "PASS" + "</a> ")
                .append(d.appName)
                .append(" <span class=\"badge badge-pill badge-secondary\">" + d.version + "</span>");
        } else if (d.labels.includes(LABEL_QUEUE)) {
            cardHeaderTitle.append("<span class=\"badge badge-pill badge-info\">MESSAGE QUEUE</span>")
                .append(" " + d.queueName);
        }

        // Info tab
        if (d.labels.includes(LABEL_ENDPOINT)) {
        } else if (d.labels.includes(LABEL_SERVICE) && !d.labels.includes(LABEL_NULLSERVICE)) {
            fetch("/web-page/app/swagger/" + d.appId)
                .then(response => response.json())
                .then(json => {
                    nodeInfoBody.append("<a class='card-subtitle' href='http://" + json.host + "/swagger-ui.html' target='_blank'>Swagger UI</a>");
                    for (let key in json.info) {
                        if (key !== "version" && key !== "title") {
                            nodeInfoBody.append("<h5 class=\"card-title\">" + key.charAt(0).toUpperCase() + key.slice(1) + "</h5>");
                            nodeInfoBody.append(json.info[key]);
                        }
                    }
                    startMonitor(json.host);
                });
        }

        // Graph tab
        graphProvider.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/providers/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        graphConsumers.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/consumers/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        graphUpperDependencyStrong.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/strong-upper-dependency-chain/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        graphUpperDependencyWeak.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/weak-upper-dependency-chain/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        graphLowerDependencyStrong.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/strong-lower-dependency-chain/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        graphLowerDependencyWeak.on("click", function () {
            if (!$(this).hasClass("active")) {
                $(this).parent().find(".active").removeClass("active");
                $(this).addClass("active");
                fetch("/web-page/graph/weak-lower-dependency-chain/" + d.id)
                    .then(response => response.json())
                    .then(json => {
                        highlight(json);
                    });
            } else {
                $(this).removeClass("active");
                clearHighlight();
            }
        });

        // Monitor
        function startMonitor(host) {
            fetch("http://" + host + "/health")
                .then(response => response.json())
                .then(json => {
                    healthJson.jsonViewer(json, {collapsed: true, withQuotes: false});
                });

            fetch("http://" + host + "/metrics")
                .then(response => response.json())
                .then(json => {
                    metricsActuratorJson.jsonViewer(json, {collapsed: true, withQuotes: false});
                });

            fetch("/web-page/app/metrics/log/" + d.appId)
                .then(response => response.json())
                .then(json => {
                    metricsElasticsearchJson.jsonViewer(json, {collapsed: true, withQuotes: false});
                });
        }


        if (d.labels.includes(LABEL_ENDPOINT)) {
        } else if (d.labels.includes(LABEL_SERVICE) && !d.labels.includes(LABEL_NULLSERVICE)) {
            fetch("/web-page/monitor/getErrors/" + d.systemName)
                .then(response => response.json())
                .then(json => {
                    console.log("monitor/getErrors/" + d.systemName);

                    monitorErrorGroup.append("<div id=\"" + d.appName.toUpperCase() + "-error500\" style=\"display:none;\"><h4 class=\"card-monitorError\"><span>" + "Error Detect (500)" + "</span></h4></div>");
                    monitorErrorGroup.append("<div id=\"" + d.appName.toUpperCase() + "-error502\" style=\"display:none;\"><h4 class=\"card-monitorError\"><span>" + "Error Detect (502)" + "</span></h4></div>");
                    monitorErrorGroup.append("<div id=\"" + d.appName.toUpperCase() + "-error503\" style=\"display:none;\"><h4 class=\"card-monitorError\"><span>" + "Error Detect (503)" + "</span></h4></div>");
                    monitorErrorGroup.append("<div id=\"" + d.appName.toUpperCase() + "-error504\" style=\"display:none;\"><h4 class=\"card-monitorError\"><span>" + "Error Detect (504)" + "</span></h4></div>");


                    for(let everyError = 0;everyError < Object.keys(json).length; everyError++){
                        let index = json[everyError]["index"];
                        let jsonErr = json[everyError];
                        let errrrr = everyError;
                        let errorAppName = json[everyError]["errorAppName"];
                        let errorAppVersion = json[everyError]["errorAppVersion"];
                        let consumerAppName = json[everyError]["consumerAppName"];
                        let timestamp = json[everyError]["timestamp"];
                        let statusCode = json[everyError]["statusCode"];
                        let errorMessage = json[everyError]["errorMessage"];
                        let errorPath = json[everyError]["errorPath"];
                        let testedPASS = json[everyError]["testedPASS"];




                        let iddd = errorAppName.toUpperCase() + "-error" + statusCode;
                        let iddd2 = "error-" + index;

                        // document.getElementById(iddd).innerHTML += "<button class=\"list-group-item list-group-item-action list-group-item-danger\" id=\"" + iddd2 + "\" >" + "error - " + index + " - " + errorPath + "</button>";
                        if(testedPASS === true)
                            $('#' + iddd).append("<button class=\"list-group-item list-group-item-action list-group-item-danger\" id=\"" + iddd2 + "\" >" + "error - " + index + " - " + errorPath + " - testedPASS" + "</button>");
                        else
                            $('#' + iddd).append("<button class=\"list-group-item list-group-item-action list-group-item-danger\" id=\"" + iddd2 + "\" >" + "error - " + index + " - " + errorPath + "</button>");
                        // document.getElementById(iddd).style.display = "block";
                        $('#' + iddd).show();

                    }

                    // jquery跟上面寫在一起會導致事件繫結不起來
                    for(let everyError = 0;everyError < Object.keys(json).length; everyError++){
                        $('#error-' + everyError).bind("click", {index:everyError, jsonContent:json[everyError]}, clickHandler);
                    }
                })
        }

        function clickHandler(event) {
            let index_everyError = event.data.index;
            let json_content = event.data.jsonContent;

            let errorId = "error-" + index_everyError;

            if (!$('#' + errorId).hasClass("active")) {
                $('#' + errorId).parent().find(".active").removeClass("active");
                monitorErrorMessage.removeClass("show");
                monitorError_feedbackContract.removeClass("show");
                $('#' + errorId).addClass("active");
                monitorErrorMessage.addClass("show");
                monitorError_feedbackContract.addClass("show");

                monitorErrorMessageJson.jsonViewer(json_content, {collapsed: true, withQuotes: false});

                if(json_content["errorType"] !== "RequestError") {

                    monitorError_feedbackContract.show();
                    monitorError_feedbackContract.empty();
                    monitorError_feedbackContract.append("<h4 class=\"card-feedbackContract\"><span>" + "Feedback Contract：" + json_content["consumerAppName"] + ".groovy" + "</span></h4>");

                    let feedbackContract = "";
                    feedbackContract += "<span class='span-feedbackContract'>";
                    feedbackContract += "Contract.<span class='purple'>make</span> {<br>" +
                        "&nbsp;&nbsp;&nbsp;description (<span class='green'>\"\"</span>)<br>" +
                        "&nbsp;&nbsp;&nbsp;name (<span class='green'>\"\"</span>)<br>" +
                        "&nbsp;&nbsp;&nbsp;request {<br>" +
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;method (<span class='green'>\"" + json_content["errorMethod"] + "\"</span>)<br>" +
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;url (<span class='green'>\"" + json_content["errorPath"] + "\"</span>)";

                    let errorUrl = json_content["errorUrl"];
                    let queryParameters = errorUrl.split(json_content["errorPath"] + "?");
                    if (queryParameters.length !== 0) {
                        feedbackContract += "&nbsp;{<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;queryParameters {<br>";

                        let parameters = queryParameters[1].split("&");
                        for (let parameter in parameters) {
                            let parameterAndValue = parameters[parameter].split("=");
                            feedbackContract += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parameter(<span class='green'>\"" + parameterAndValue[0] + "\"</span>,<span class='green'>\"" + parameterAndValue[1] + "\"</span>)<br>";
                        }

                        feedbackContract += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>";

                    } else {
                        feedbackContract += "<br>";
                    }

                    feedbackContract += "&nbsp;&nbsp;&nbsp;}<br>";
                    feedbackContract += "&nbsp;&nbsp;&nbsp;response {<br>" +
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; status(<span class='blue'>" + "200" + "</span>)<br>" +
                        "&nbsp;&nbsp;&nbsp;}<br>" +
                        "}";
                    feedbackContract += "</span>";
                    monitorError_feedbackContract.append(feedbackContract);
                }else {
                    monitorError_feedbackContract.empty();
                    monitorError_feedbackContract.hide();
                }

                let highlightJson = "";
                highlightJson += "{";

                // 要highlight的nodes
                highlightJson += "\"nodes\":[";

                for(let errorService in json_content["errorServices"]){
                    highlightJson += "{\"id\":" + json_content["errorServices"][errorService]["id"] + "}";
                    highlightJson += ",";
                }
                for(let errorEndpoint in json_content["errorEndpoints"]){
                    highlightJson += "{\"id\":" + json_content["errorEndpoints"][errorEndpoint]["id"] + "}";
                    highlightJson += ",";
                }

                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "]";
                highlightJson += ",";

                // 要highlight的links
                highlightJson += "\"links\":[";
                for(let errorLink in json_content["errorLinks"]){
                    highlightJson += "{\"source\":" + json_content["errorLinks"][errorLink]["aid"] + ",\"type\":\"" + json_content["errorLinks"][errorLink]["relationship"] + "\",\"target\":" + json_content["errorLinks"][errorLink]["bid"] + "}";
                    highlightJson += ",";
                }

                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "]";
                highlightJson += ",";

                // 要highlight的sourceNodes
                highlightJson += "\"sourceNodes\":[";
                for(let errorService in json_content["errorServices"]){
                    if(json_content["errorServices"][errorService]["sourceOfError"] === true){
                        highlightJson += "{\"id\":" + json_content["errorServices"][errorService]["id"] + "}";
                        highlightJson += ",";
                    }
                }
                for(let errorEndpoint in json_content["errorEndpoints"]){
                    if(json_content["errorEndpoints"][errorEndpoint]["sourceOfError"] === true) {
                        highlightJson += "{\"id\":" + json_content["errorEndpoints"][errorEndpoint]["id"] + "}";
                        highlightJson += ",";
                    }
                }
                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "]";
                highlightJson += ",";


                // 要highlight的sourceLinks
                highlightJson += "\"sourceLinks\":[";
                for(let errorLink in json_content["errorLinks"]){
                    if(json_content["errorLinks"][errorLink]["sourceOfError"] === true) {
                        highlightJson += "{\"source\":" + json_content["errorLinks"][errorLink]["aid"] + ",\"type\":\"" + json_content["errorLinks"][errorLink]["relationship"] + "\",\"target\":" + json_content["errorLinks"][errorLink]["bid"] + "}";
                        highlightJson += ",";
                    }
                }

                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "]";


                highlightJson += "}";

                let highlighttoJson = JSON.parse(highlightJson);
                console.log("highlightJson: " + highlightJson);
                highlight_error(highlighttoJson);
            } else {
                $('#' + errorId).removeClass("active");
                clearHighlight();
                monitorErrorMessage.removeClass("show");
                monitorError_feedbackContract.removeClass("show");
            }
        }


        // Contract Tab
        // Contract Testing Result
        if (d.labels.includes(LABEL_ENDPOINT)) {
        } else if (d.labels.includes(LABEL_SERVICE) && !d.labels.includes(LABEL_NULLSERVICE)) {
            fetch("/web-page/graph/providers/" + d.id)
                .then(response => response.json())
                .then(json => {
                    let parentNode = new Array();
                    json.nodes.forEach(node => {


                        let parentNodeTemp = findParentById(node.id);
                        if (parentNode.indexOf(parentNodeTemp.id) !== -1)
                            return;
                        else
                            parentNode.push(parentNodeTemp.id);

                        fetch("/web-page/app/swagger/" + parentNodeTemp.appId)
                            .then(response => response.json())
                            .then(json2 => {
                                contractGroup.append("<h4 class=\"card-contract\"><span>" + json2["info"]["title"].toUpperCase() + "</span></h4>");

                                let contractContent = json2["x-contract"][d.appName.toLowerCase() + ".groovy"];

                                // for( let api in contractContent)
                                for(let api in contractContent){

                                    for(let index in contractContent[api]) {

                                        if (contractContent[api][index]["testResult"]["status"] === "PASS") {
                                            contractGroup.append("<button class=\"list-group-item list-group-item-action list-group-item-success\" id=\"contract-" + api.substring(1).replace("/", "-") + index + "\">" + api + "</button>");

                                        } else {
                                            document.getElementById('serviceCondition').setAttribute("class", "badge badge-pill badge-warning");
                                            document.getElementById('serviceCondition').innerText = "WARNING";
                                            contractGroup.append("<button class=\"list-group-item list-group-item-action list-group-item-danger\" id=\"contract-" + api.substring(1).replace("/", "-") + index + "\">" + api + "</button>");
                                        }
                                    }

                                }

                                for(let api in contractContent){
                                    for(let index in contractContent[api]) {
                                        // $('#error-' + everyError).bind("click", {index:everyError, jsonContent:json[everyError]}, clickHandler);
                                        $('#contract-' + api.substring(1).replace("/", "-") + index).bind("click", {
                                            index: index,
                                            index_api: api,
                                            jsonContent: contractContent,
                                            consumerServiceId: d.id,
                                            providerServiceAppName: json2["info"]["title"].toUpperCase(),
                                            providerServiceAppVersion: json2["info"]["version"]
                                        }, clickHandler2);
                                    }
                                }
                            });

                    });
                });
        }

        // Missing Contract
        if (d.labels.includes(LABEL_ENDPOINT)) {
        } else if (d.labels.includes(LABEL_SERVICE) && !d.labels.includes(LABEL_NULLSERVICE)) {
            fetch("/web-page/app/swagger/" + d.appId)
                .then(response => response.json())
                .then(json => {
                    if(json["x-serviceDependency"]["httpRequest"]){
                        let httpRequestTarget = json["x-serviceDependency"]["httpRequest"];
                        console.log("d.appId: " + d.appId);
                        for( let api in httpRequestTarget){
                            let method = Object.keys(httpRequestTarget[api])[0];
                            console.log("api: " + api);
                            console.log("httpRequestTarget[api]: " + httpRequestTarget[api]);
                            console.log("method: " + method);



                            for(let targetService in httpRequestTarget[api][method]["targets"]){
                                let targetVersion = Object.keys(httpRequestTarget[api][method]["targets"][targetService])[0];
                                let targetApi = Object.keys(httpRequestTarget[api][method]["targets"][targetService][targetVersion])[0];

                                console.log("targetService: " + targetService);
                                console.log("targetVersion: " + targetVersion);
                                console.log("targetApi: " + targetApi);

                                let targetAppId = d.systemName + ":" + targetService.toUpperCase() + ":" + targetVersion;

                                console.log("targetAppId: " + targetAppId);
                                if ($('#contractMissing-' + targetService.toUpperCase()).length <= 0) { // id不存在
                                    contractMissingGroup.append("<h4 id=\"contractMissing-" + targetService.toUpperCase() + "\" class=\"card-contract\"><span>" + targetService.toUpperCase() + "</span></h4>");
                                    $('#contractMissing-' + targetService.toUpperCase()).hide();
                                }


                                fetch("/web-page/app/swagger/" + targetAppId)
                                    .then(response2 => response2.json())
                                    .then(json2 => {
                                        let contractContent = json2["x-contract"];
                                        let groovyName = d.appName.toLowerCase() + ".groovy";
                                        if(!contractContent[groovyName] || !contractContent[groovyName][targetApi]){
                                            console.log("12332132123132132131");
                                            $('#contractMissing-' + targetService.toUpperCase()).after("<button class=\"list-group-item list-group-item-action list-group-item-warning\" id=\"contractMissing-" + json["info"]["title"].toUpperCase() + "-" + targetApi.substring(1)  + "\">" + targetApi + "</button>");
                                            $('#contractMissing-' + targetService.toUpperCase()).show();
                                        }
                                        if(!contractContent[groovyName] || !contractContent[groovyName][targetApi]){
                                            console.log("clickkkkkkkkkkkkkkkkk: contractMissing-" + json["info"]["title"].toUpperCase() + "-" + targetApi.substring(1));
                                            $('#contractMissing-' + json["info"]["title"].toUpperCase() + '-' + api.substring(1)).bind("click", {
                                                consumerServiceJsonContent: json,
                                                consumerServiceId: d.id,
                                                consumerServiceAppName: json["info"]["title"].toUpperCase(),
                                                consumerServiceAppVersion: json["info"]["version"],
                                                consumerService_api: api,
                                                providerServiceJsonContent: json2,
                                                providerServiceAppName: json2["info"]["title"].toUpperCase(),
                                                providerServiceAppVersion: json2["info"]["version"],
                                                providerService_api: targetApi
                                            }, clickHandler3);
                                        }
                                    });
                            }
                        }
                    }
                });
        }


        function clickHandler2(event) {
            let index = event.data.index;
            let index_api = event.data.index_api;
            let json_content = event.data.jsonContent;
            let providerServiceAppName = event.data.providerServiceAppName;
            let providerServiceAppVersion = event.data.providerServiceAppVersion;
            let providerService = data.nodes.find(node => (node.appName === providerServiceAppName) && (node.version === providerServiceAppVersion));
            // let providerEndpoint = data.nodes.find(node => (node.appName === providerServiceAppName) && (node.labels.includes(LABEL_ENDPOINT)) && (node.path === index_api));
            let providerEndpoint = "";
            data.nodes.filter(node => (node.appName === providerServiceAppName) && (node.labels.includes(LABEL_ENDPOINT)) && ((node.path === index_api) || (node.path === index_api.substring(0,index_api.lastIndexOf("_"))) ))
                .forEach(nodeWithVersion => {
                    data.links.filter(link => (link.type === REL_OWN) && (link.source.id === providerService.id))
                        .forEach(nce2 => {
                            if(nce2.target.id === nodeWithVersion.id)
                                providerEndpoint = nodeWithVersion;
                        });
                });
            let providerServiceId = providerService.id;
            let providerEndpointId = providerEndpoint.id;
            let consumerServiceId = event.data.consumerServiceId;

            let apiId = "contract-" + index_api.substring(1).replace("/","-") + index;

            if (!$('#' + apiId).hasClass("active")) {
                $('#' + apiId).parent().find(".active").removeClass("active");
                extraMessage.removeClass("show");
                $('#' + apiId).addClass("active");
                extraMessage.addClass("show");

                messageJson.jsonViewer(json_content[index_api][index], {collapsed: false, withQuotes: false});

                // 要highlight的nodes, links
                let highlightJson = "";
                highlightJson += "{";

                // 要highlight的nodes
                highlightJson += "\"nodes\":[";

                // node: provider endpoint
                highlightJson += "{\"id\":" + providerEndpointId + "}";
                highlightJson += ",";

                highlightJson += "{\"id\":" + providerServiceId + "}";
                highlightJson += ",";

                // node: consumer parent
                highlightJson += "{\"id\":" + consumerServiceId + "}";
                highlightJson += ",";

                // nodes: consumer endpoint (可能不只一個)
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id);
                                highlightJson += "{\"id\":" + dnfTemp.id + "}";
                                highlightJson += ",";
                            });
                        }
                    });
                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;

                highlightJson += "]";
                highlightJson += ",";

                // 要highlight的links
                highlightJson += "\"links\":[";

                // link: consumer parent -OWN- consumer endpoint
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id);
                                highlightJson += "{\"source\":" + consumerServiceId + ",\"type\":\"" + REL_OWN + "\",\"target\":" + dnfTemp.id + "}";
                                highlightJson += ",";
                            });
                        }else {
                            return;
                        }


                    });

                // link: consumer endpoint -HTTPREQUEST-> provider endpoint
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id);
                                highlightJson += "{\"source\":" + dnfTemp.id + ",\"type\":\"" + REL_HTTPREQUEST + "\",\"target\":" + providerEndpointId + "}";
                                highlightJson += ",";
                            });
                        }else {
                            return;
                        }


                    });

                // link: provider parent -OWN- provider endpoint
                highlightJson += "{\"source\":" + providerServiceId + ",\"type\":\"" + REL_OWN + "\",\"target\":" + providerEndpointId + "}";
                highlightJson += "]";
                highlightJson += ",";


                // 要highlight的sourceNodes
                highlightJson += "\"sourceNodes\":[]";
                highlightJson += ",";

                // 要highlight的sourceLinks
                highlightJson += "\"sourceLinks\":[]";
                // highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "}";

                let highlighttoJson = JSON.parse(highlightJson);
                console.log(highlighttoJson);
                // highlight(highlighttoJson);

                if (json_content[index_api][index]["testResult"]["status"] === "PASS")
                    highlight(highlighttoJson);
                else
                    highlight_error(highlighttoJson);


            } else {
                $('#' + apiId).removeClass("active");
                clearHighlight();
                extraMessage.removeClass("show");
            }
        }

        function clickHandler3(event) {

            let consumerServiceJsonContent = event.data.consumerServiceJsonContent;
            let consumerServiceId = event.data.consumerServiceId;
            let consumerServiceAppName = event.data.consumerServiceAppName;
            let consumerServiceAppVersion = event.data.consumerServiceAppVersion;
            let consumerService_api = event.data.consumerService_api;
            let providerServiceJsonContent = event.data.providerServiceJsonContent;
            let providerServiceAppName = event.data.providerServiceAppName;
            let providerServiceAppVersion = event.data.providerServiceAppVersion;
            let providerService_api = event.data.providerService_api;
            let providerService = data.nodes.find(node => (node.appName === providerServiceAppName) && (node.version === providerServiceAppVersion));
            let providerEndpoint = "";
            data.nodes.filter(node => (node.appName === providerServiceAppName) && (node.labels.includes(LABEL_ENDPOINT)) && ((node.path === providerService_api) || (node.path === providerService_api.substring(0,providerService_api.lastIndexOf("_"))) ))
                .forEach(nodeWithVersion => {
                    data.links.filter(link => (link.type === REL_OWN) && (link.source.id === providerService.id))
                        .forEach(nce2 => {
                            if(nce2.target.id === nodeWithVersion.id)
                                providerEndpoint = nodeWithVersion;
                        });
                });
            let providerServiceId = providerService.id;
            let providerEndpointId = providerEndpoint.id;

            console.log("providerServiceAppName: " + providerServiceAppName);
            console.log("providerServiceAppVersion: " + providerServiceAppVersion);
            console.log("providerEndpoint.path.substring(1): " + providerEndpoint.path.substring(1));
            console.log("consumerServiceJsonContent[\"info\"][\"title\"].toUpperCase(): " + consumerServiceJsonContent["info"]["title"].toUpperCase());

            let buttonId = "contractMissing-" + consumerServiceJsonContent["info"]["title"].toUpperCase() + '-' + providerEndpoint.path.substring(1);

            console.log("buttonId: " + buttonId);

            if(!$('#' + buttonId).hasClass("active")){
                // $('#' + buttonId).parent().find(".active").removeClass("active");
                // extraMessage.removeClass("show");
                $('#' + buttonId).addClass('active');
                // extraMessage.addClass("show");

                // 要highlight的nodes, links
                let highlightJson = "";
                highlightJson += "{";

                // 要highlight的nodes
                highlightJson += "\"nodes\":[";
                // node: provider endpoint
                highlightJson += "{\"id\":" + providerEndpointId + "}";
                highlightJson += ",";

                highlightJson += "{\"id\":" + providerServiceId + "}";
                highlightJson += ",";

                // node: consumer parent
                highlightJson += "{\"id\":" + consumerServiceId + "}";
                highlightJson += ",";

                // nodes: consumer endpoint
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id && dnf.path === consumerService_api);
                                highlightJson += "{\"id\":" + dnfTemp.id + "}";
                                highlightJson += ",";
                            });
                        }
                    });
                highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;

                highlightJson += "]";
                highlightJson += ",";

                // 要highlight的links
                highlightJson += "\"links\":[";

                // link: consumer parent -OWN- consumer endpoint
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id);
                                highlightJson += "{\"source\":" + consumerServiceId + ",\"type\":\"" + REL_OWN + "\",\"target\":" + dnfTemp.id + "}";
                                highlightJson += ",";
                            });
                        }else {
                            return;
                        }


                    });

                // link: consumer endpoint -HTTPREQUEST-> provider endpoint
                data.links.filter(link => (link.type === REL_OWN) && (link.source.id === consumerServiceId))
                    .forEach(nce2 => {
                        let dlff = data.links.filter(dlf => (dlf.type === REL_HTTPREQUEST) && (dlf.source.id === nce2.target.id) && (dlf.target.id === providerEndpointId));
                        if (dlff !== null && dlff !== undefined){
                            dlff.forEach(dlfff => {
                                let dnfTemp = data.nodes.find(dnf => dnf.id === dlfff.source.id);
                                highlightJson += "{\"source\":" + dnfTemp.id + ",\"type\":\"" + REL_HTTPREQUEST + "\",\"target\":" + providerEndpointId + "}";
                                highlightJson += ",";
                            });
                        }else {
                            return;
                        }


                    });

                // link: provider parent -OWN- provider endpoint
                highlightJson += "{\"source\":" + providerServiceId + ",\"type\":\"" + REL_OWN + "\",\"target\":" + providerEndpointId + "}";
                highlightJson += "]";
                highlightJson += ",";


                // 要highlight的sourceNodes
                highlightJson += "\"sourceNodes\":[]";
                highlightJson += ",";

                // 要highlight的sourceLinks
                highlightJson += "\"sourceLinks\":[]";
                // highlightJson = (highlightJson.substring(highlightJson.length-1)==',')?highlightJson.substring(0,highlightJson.length-1):highlightJson;
                highlightJson += "}";

                let highlighttoJson = JSON.parse(highlightJson);
                console.log(highlighttoJson);
                highlight(highlighttoJson);


            }else {
                $('#' + buttonId).removeClass('active');
                clearHighlight();
                // extraMessage.removeClass("show");
            }
        }


        // Alert
        // Init alert form
        fetch("/web-page/app/setting/" + d.appId)
            .then(response => response.json())
            .then(json => {
                // Init failure alert inputs
                if (!isNaN(json.failureStatusRate)) {
                    failureStatusRateInput.val(json.failureStatusRate * 100).trigger("input");
                } else {
                    failureStatusRateInput.val(100).trigger("input");
                }
                if (!isNaN(json.failureErrorCount)) {
                    failureErrorCountInput.val(json.failureErrorCount);
                } else {
                    failureErrorCountInput.val("");
                }
                if (json.enableRestFailureAlert) {
                    enableRestFailureAlertInput.prop("checked", true);
                } else {
                    enableRestFailureAlertInput.prop("checked", false);
                }
                if (json.enableLogFailureAlert) {
                    enableLogFailureAlertInput.prop("checked", true);
                } else {
                    enableLogFailureAlertInput.prop("checked", false);
                }
                // Init SPC high duration rate inputs
                if (!isNaN(json.thresholdSPCHighDurationRate)) {
                    thresholdSPCHighDurationRateInput.val(json.thresholdSPCHighDurationRate * 100).trigger("input");
                } else {
                    thresholdSPCHighDurationRateInput.val(100).trigger("input");
                }
                if (json.enableSPCHighDurationRateAlert) {
                    enableSPCHighDurationRateAlertInput.prop("checked", true);
                } else {
                    enableSPCHighDurationRateAlertInput.prop("checked", false);
                }
                // Init average duration inputs
                if (!isNaN(json.thresholdAverageDuration)) {
                    thresholdAverageDurationInput.val(json.thresholdAverageDuration).trigger("input");
                } else {
                    thresholdAverageDurationInput.val("");
                }
                if (json.enableRestAverageDurationAlert) {
                    enableRestAverageDurationAlertInput.prop("checked", true);
                } else {
                    enableRestAverageDurationAlertInput.prop("checked", false);
                }
                if (json.enableLogAverageDurationAlert) {
                    enableLogAverageDurationAlertInput.prop("checked", true);
                } else {
                    enableLogAverageDurationAlertInput.prop("checked", false);
                }
                // Init strong dependency alert inputs
                if (!isNaN(json.strongUpperDependencyCount)) {
                    strongUpperDependencyCountInput.val(json.strongUpperDependencyCount);
                } else {
                    strongUpperDependencyCountInput.val("");
                }
                if (!isNaN(json.strongLowerDependencyCount)) {
                    strongLowerDependencyCountInput.val(json.strongLowerDependencyCount);
                } else {
                    strongLowerDependencyCountInput.val("");
                }
                if (json.enableStrongDependencyAlert) {
                    enableStrongDependencyAlertInput.prop("checked", true);
                } else {
                    enableStrongDependencyAlertInput.prop("checked", false);
                }
                // Init weak dependency alert inputs
                if (!isNaN(json.weakUpperDependencyCount)) {
                    weakUpperDependencyCountInput.val(json.weakUpperDependencyCount);
                } else {
                    weakUpperDependencyCountInput.val("");
                }
                if (!isNaN(json.weakLowerDependencyCount)) {
                    weakLowerDependencyCountInput.val(json.weakLowerDependencyCount);
                } else {
                    weakLowerDependencyCountInput.val("");
                }
                if (json.enableWeakDependencyAlert) {
                    enableWeakDependencyAlertInput.prop("checked", true);
                } else {
                    enableWeakDependencyAlertInput.prop("checked", false);
                }
                if (!isNaN(json.riskValueAlert)) {
                    riskValueAlertInput.val(json.riskValueAlert*1.0).trigger("input");
                } else {
                    riskValueAlertInput.val(1.0).trigger("input");
                }
                if (json.enableRiskValueAlert) {
                    enableRiskValueAlertInput.prop("checked", true);
                } else {
                    enableRiskValueAlertInput.prop("checked", false);
                }
            }).catch(error => {
            console.error("Error:", error)
        });

        // Loop over them and prevent submission
        let validation = Array.prototype.filter.call(nodeSettingforms, function(form) {
            $(form).submit(async function (event) {
                event.preventDefault();
                event.stopPropagation();
                if (form.checkValidity() !== false) {
                    let data = {};
                    nodeSettingforms.find("input").each((index, input) => {
                        if (input.type === "range" && input.id === "risk-value-alert") {
                            data[input.name] = input.value * 1.0;
                        }else if (input.type === "range") {
                            data[input.name] = input.value * 0.01;
                        } else if (input.type === "checkbox") {
                            data[input.name] = input.checked;
                        }else {
                            data[input.name] = input.value;
                        }
                    });
                    await fetch("/web-page/app/setting/" + d.appId, {
                        method: "post",
                        body: JSON.stringify(data),
                        headers: new Headers({
                            "Content-Type": "application/json"
                        })
                    }).then(res => res.json())
                        .catch(error => {
                            toast.find("i").remove();
                            toast.find(".toast-header")
                                .attr("class", "toast-header text-white bg-warning")
                                .prepend("<i class='fas fa-bug mr-2'></i>");
                            toast.find("strong").empty().append("Setting failed");
                            toast.find(".toast-body").empty().append("The setting for <strong>" + d.appName + ":" + d.version + "</strong> was failed.");
                            toast.toast('show');
                            console.error("Error:", error)
                        })
                        .then(response => {
                            toast.find("i").remove();
                            toast.find(".toast-header")
                                .attr("class", "toast-header text-white bg-primary")
                                .prepend("<i class='fas fa-info-circle mr-2'></i>");
                            toast.find("strong").empty().append("Setting updated");
                            toast.find(".toast-body").empty().append("The setting for <strong>" + d.appName + ":" + d.version + "</strong> has been successfully updated.");
                            toast.toast('show');
                            console.log("Success", response);
                        })
                        .then(response2 => {
                            // reload
                            setTimeout(function() {
                                clearHighlight();
                                extraMessage.removeClass("show");
                                fetch("/web-page/graph/getGraphJson/" + d.systemName)
                                    .then(response => response.json())
                                    .then(graphJson => {
                                        graphData = graphJson;
                                        update(emptyData);
                                        update(graphData);
                                    });
                            }, 10000);
                        });



                }
                form.classList.add('was-validated');

            });

        });

        // Show
        cardDiv.addClass("show");
    }


    this.closeNodeCard = function() {
        cardClose.click();
    };

    // Collapse Graph
    $("#graph-enable-collapse").on("change", function () {
        if (this.checked) {
            graphData = collapseData;
            update(emptyData);
            update(graphData);
        } else {
            clearHighlight();
            extraMessage.removeClass("show");
            fetch("/web-page/graph/getGraphJson/" + data.nodes[0].systemName)
                .then(response => response.json())
                .then(graphJson => {
                    graphData = graphJson;
                    update(emptyData);
                    update(graphData);
                });

            /*            graphData = data;
                        update(emptyData);
                        update(graphData);*/
        }
    });


    this.clickNodeByNameAndVersion = function(appName, version) {
        node.filter(d => d.appName === appName && d.version === version).dispatch("click");
    };

    this.stopSimulation = function () {
        simulation.stop();
    };

    this.restartSimulation = function () {
        simulation.restart();
    };

}


