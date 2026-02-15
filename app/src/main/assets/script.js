import fs from "fs";

export const textToImage = async (theme) => {
 const path =
   "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image";

 const headers = {
   Accept: "application/json",
   Authorization: "sk-vCpXQC4ZlbAFdtafncOZSVvlIgkhFuxDqk4dSVLrsBVsVR34"
 };

 const body = {
   steps: 40,
   width: 1024,
   height: 1024,
   seed: 0,
   cfg_scale: 5,
   samples: 1,
   text_prompts: [
     {
       "text": theme,
       "weight": 1
     },
     {
       "text": "blurry, bad",
       "weight": -1
     }
   ],
 };

 const response = fetch(
   path,
   {
     headers,
     method: "POST",
     body: JSON.stringify(body),
   }
 );

 if (!response.ok) {
   throw new Error(`Non-200 response: ${await response.text()}`)
 }

 const responseJSON = await response.json();

 let imageUrl = "";
 responseJSON.artifacts.forEach((image, index) => {
   fs.writeFileSync(
     `./out/txt2img_${image.seed}.png`,
     Buffer.from(image.base64, 'base64')
   )
   imageUrl = `file:///android_asset/out/txt2img_${image.seed}.png`;
 })

 return imageUrl;
};
