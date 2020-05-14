function SPCGraph(divId, data) {
    this.divId = divId;
    this.data = data;

    let apps = Object.keys(data.values);
    let values = Object.values(data.values);
    let viols = this.getViols(data.values, data.violations, data.ucl, data.lcl);
    let clStart = Object.keys(data.values)[0];
    let clEnd = Object.keys(data.values)[Object.keys(data.values).length - 1];
    let sdRange = data.ucl - data.cl;
    let uLimit = data.ucl + sdRange;
    let lLimit = data.lcl - sdRange;
    if (lLimit < 0) { lLimit = 0 }

    // data
    let Data = {
        type: 'scatter',
        x: apps,
        y: values,
        mode: 'markers',
        name: 'Data',
        showlegend: true,
        hoverinfo: 'all',

        line: {
            simplify: false,
            color: 'blue',
            width: 2
        },
        marker: {
            colorscale: 'Portland',
            color: values,
            size: 12,
            symbol: 'circle'
        }
    };

    // violations
    let Viol = {
        type: 'scatter',
        x: Object.keys(viols),
        y: Object.values(viols),
        mode: 'markers',
        name: 'Violation',
        showlegend: true,
        marker: {
            color: 'rgb(38, 38, 38)',
            line: {width: 5},
            opacity: 1,
            size: 20,
            symbol: 'circle-open'
        }
    };

    // control limits
    let CL = {
        type: 'scatter',
        x: [clStart, clEnd, null, clStart, clEnd],
        y: [data.lcl, data.lcl, null, data.ucl, data.ucl],
        mode: 'lines',
        name: 'LCL/UCL',
        showlegend: true,
        line: {
            simplify: false,
            color: '#F6697D',
            width: 1,
            dash: 'dash'
        }
    };

    // centre
    let Centre = {
        type: 'scatter',
        x: [clStart, clEnd],
        y: [data.cl, data.cl],
        mode: 'lines',
        name: 'Centre',
        showlegend: true,
        line: {
            simplify: false,
            color: '#CFC5C9',
            width: 1
        }
    };

    // histogram on axis 2
    let histo = {
        type: 'histogram',
        x: apps,
        y: values,
        name: 'Distribution',
        orientation: 'h',
        marker: {
            color: '#3AA0E4',
            line: {
                color: 'white',
                width: 1
            }
        },
        xaxis: 'x2',
        yaxis: 'y2'
    };


    // all traces
    let plotData = [Data, Viol,CL,Centre,histo];

    // layout
    let layout = {
        title: "Control Chart - " + data.valueName,
        margin: {pad: 3},
        xaxis: {
            title: data.samplingName,
            domain: [0, 0.7], // 0 to 70% of width
            zeroline: false
        },
        yaxis: {
            title: data.valueName,
            range: [lLimit,uLimit],
            zeroline: true
        },
        xaxis2: {
            domain: [0.8, 1] // 70 to 100% of width
        },
        yaxis2: {
            range: [lLimit,uLimit],
            anchor: 'x2',
            showticklabels: false
        }
    };

    Plotly.newPlot(divId, plotData, layout, {responsive: true, showSendToCloud: true});
}

SPCGraph.prototype.getViols = function (d, viol, ucl, lcl) {
    let viols = {};
    if (Object.keys(d).length > 1) {
        if (viol.includes("ucl")) {
            for (let key in d) {
                if (d[key] > ucl) {
                    viols[key] = d[key];
                }
            }
        }

        if (viol.includes("lcl")) {
            for (let key in d) {
                if (d[key] < ucl) {
                    viols[key] = d[key];
                }
            }
        }
    }

    return viols;
};

SPCGraph.prototype.camelCaseToSentenceCase = function (text) {
    let spaced = text.replace( /([A-Z])/g, " $1" );
    return spaced.charAt(0).toUpperCase() + spaced.slice(1);
};

SPCGraph.prototype.updateData = function (data) {
    this.data = data;

    let apps = Object.keys(data.values);
    let values = Object.values(data.values);
    let viols = this.getViols(data.values, data.violations, data.ucl, data.lcl);
    let clStart = Object.keys(data.values)[0];
    let clEnd = Object.keys(data.values)[Object.keys(data.values).length - 1];
    let sdRange = data.ucl - data.cl;
    let uLimit = data.ucl + sdRange;
    let lLimit = data.lcl - sdRange;
    if (lLimit < 0) { lLimit = 0 }

    Plotly.animate(this.divId, {
        data: [{x: apps, y: values, marker: { color: values }},
            {x: Object.keys(viols), y: Object.values(viols)},
            {x: [clStart, clEnd, null, clStart, clEnd], y: [data.lcl, data.lcl, null, data.ucl, data.ucl]},
            {x: [clStart, clEnd], y: [data.cl, data.cl]},
            {x: apps, y: values}],
        traces: [0, 1, 2, 3, 4],
        layout: {}
    }, {
        transition: {
            duration: 500,
            easing: 'cubic-in-out'
        },
        frame: {
            duration: 500
        }
    });

    Plotly.animate(this.divId, {
        layout: {
            yaxis: {
                range: [lLimit,uLimit]
            },
            yaxis2: {
                range: [lLimit,uLimit]
            }
        }
    }, {
        transition: {
            duration: 500,
            easing: 'cubic-in-out'
        }
    });
};