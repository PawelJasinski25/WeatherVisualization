from fastapi import FastAPI, Response
from pdf_service import generate_report_pdf, generate_charts_zip

app = FastAPI()

@app.post("/generate-pdf")
async def generate_pdf(report_data: dict):
    pdf_bytes = generate_report_pdf(report_data)
    return Response(content=pdf_bytes, media_type="application/pdf")

@app.post("/generate-charts-zip")
async def generate_zip(report_data: dict):
    zip_bytes = generate_charts_zip(report_data)
    return Response(content=zip_bytes, media_type="application/zip")