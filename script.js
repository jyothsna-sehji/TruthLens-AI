
const messageInput = document.getElementById("message");
const charCount = document.getElementById("charCount");
const analyzeBtn = document.getElementById("analyzeBtn");
const clearBtn = document.getElementById("clearBtn");
const result = document.getElementById("result");

// Update character count
messageInput.addEventListener("input", function () {
    charCount.textContent =
        messageInput.value.length + " characters";
});

// Clear message
clearBtn.addEventListener("click", function () {
    messageInput.value = "";
    charCount.textContent = "0 characters";
    result.classList.add("hidden");
});

// Analyze message
analyzeBtn.addEventListener("click", async function () {

    const message = messageInput.value.trim();

    if (message.length < 10) {
        alert("Please enter a message containing at least 10 characters.");
        return;
    }

    analyzeBtn.disabled = true;
    analyzeBtn.textContent = "Analyzing Message...";

    try {
        const response = await fetch("/analyze", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ message: message })
        });

        if (!response.ok) {
            throw new Error("Server error");
        }

        const data = await response.json();

        displayResult(data);

    } catch (error) {
        alert("Unable to connect to TruthLens AI. Please start the Java server.");
        console.error(error);

    } finally {
        analyzeBtn.disabled = false;
        analyzeBtn.textContent = "◈ Analyze Message";
    }
});

// Display analysis result
function displayResult(data) {

    result.classList.remove("hidden");

    document.getElementById("resultTitle").textContent =
        data.category;

    document.getElementById("riskScore").textContent =
        data.riskScore;

    document.getElementById("riskBar").style.width =
        data.riskScore + "%";

    document.getElementById("riskBar").style.background =
        data.riskScore >= 70 ? "#EF5350" :
        data.riskScore >= 40 ? "#F5B942" : "#36D399";

    const reasons = document.getElementById("reasons");
    reasons.innerHTML = "";

    data.reasons.forEach(function (reason) {
        const item = document.createElement("li");
        item.textContent = reason;
        reasons.appendChild(item);
    });

    document.getElementById("recommendationText").textContent =
        data.recommendation;

    result.scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}
