let evaluating = false;

function beginEvaluation() {
    let $evaluationForm = $("#evaluationForm")

    if (!evaluating) {
        evaluating = true
        $.ajax({
            type: "POST",
            url: $evaluationForm.action,
            enctype: $evaluationForm.enctype,
            data: $evaluationForm.serialize(),
            timeout: 10000,
            success: (data) => {
                alert(data)
            },
            complete: () => {
                evaluating = false
            }
        })
    }

    return false
}