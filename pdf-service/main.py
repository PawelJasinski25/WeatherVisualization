from fastapi import FastAPI, Response
from pdf_service import generate_report_pdf

app = FastAPI()

@app.post("/generate-pdf")
async def generate_pdf(report_data: dict):
    pdf_bytes = generate_report_pdf(report_data)
    return Response(content=pdf_bytes, media_type="application/pdf")